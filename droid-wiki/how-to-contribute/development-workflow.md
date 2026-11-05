# Development workflow

The practical development cycle is source inspection, coordinated edits, host compilation and isolated runtime verification. Host jobs and transfer helpers are environment-specific examples, not a portable build pipeline (`samples/jcl/CICCMP.jcl`, `samples/proc/BUILDONL.prc`, `scripts/remote_compile.sh`).

## Trace the change boundary

1. Identify the transaction/program or batch job from [core features](../features/core/index.md) or [systems](../systems/index.md).
2. Follow `COPY`, `CALL`, CICS `LINK`/`XCTL`, file names and DD bindings. Shared navigation starts in `app/cpy/COCOM01Y.cpy`; runtime definitions are in `app/csd/CARDDEMO.CSD`.
3. Record existing behavior before changing it. Online entry writes the transaction master directly, whereas batch posting also updates account and category balances (`app/cbl/COTRN02C.cbl`, `app/cbl/CBTRN02C.cbl`). These are not equivalent paths.
4. Choose synthetic, reproducible cases and a restorable host baseline. Do not run refresh scripts to discover what they do.

## Coordinate related artifacts

| Edit | Companion review |
| --- | --- |
| Screen field | BMS source, generated symbolic map, receive/populate logic and consumers of appended COMMAREA state (`app/bms/COACTUP.bms`, `app/cpy-bms/COACTUP.CPY`, `app/cbl/COACTUPC.cbl`) |
| Record field or key | All copybook consumers, allocation lengths/keys, alternate indexes, SORT offsets and transfer encoding (`app/cpy/CVACT01Y.cpy`, `app/jcl/ACCTFILE.jcl`) |
| Menu target | Table count/shape, caller rebuild, installed program and transaction (`app/cpy/COMEN02Y.cpy`, `app/cbl/COMEN01C.cbl`, `app/csd/CARDDEMO.CSD`) |
| SQL field | DDL, DCLGEN, SQL mappings, extracts and package/plan bindings (`app/app-transaction-type-db2/ddl/TRNTYPE.ddl`, `samples/proc/BLDCIDB2.prc`) |
| IMS segment | DBD/PSB, copybook, online buffers, loader/unloader and purge (`app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd`) |
| Report selection | Embedded online JCL, standalone job and procedure (`app/cbl/CORPT00C.cbl`, `app/jcl/TRANREPT.jcl`, `app/proc/TRANREPT.prc`) |

Preserve fixed-format columns, field widths, signed/packed representations and leading zeros. Avoid broad whitespace normalization; [patterns and conventions](patterns-and-conventions.md) documents source conventions.

## Build on the target runtime

For BMS changes, generate both the loadable map and symbolic copybook, then recompile callers. `samples/jcl/BMSCMP.jcl` invokes `samples/proc/BUILDBMS.prc`; `samples/jcl/CICCMP.jcl` invokes `samples/proc/BUILDONL.prc`. Batch compilation uses `samples/jcl/BATCMP.jcl` and `samples/proc/BUILDBAT.prc`.

Db2 programs additionally need the SQL precompile, CICS translation, link and bind path in `samples/jcl/CICDBCMP.jcl` and `samples/proc/BLDCIDB2.prc`. IMS/MQ compilation has its own translator options, libraries and MQ stub in `samples/jcl/IMSMQCMP.jcl`. Adapt symbols, libraries and resource associations to the authorized host; these are host-only procedures, not commands for a laptop shell.

Inspect each compile/translator/binder/bind result and listing. Confirm the intended member reached the load library and that the region activated the intended program/map version. Some sample jobs issue NEWCOPY; compilation alone is not proof that a running task uses the new module (`samples/jcl/CICCMP.jcl`, `samples/jcl/BMSCMP.jcl`).

## Verify and submit

Run read-only navigation/inquiries first, then confirmation/cancel, writes and failure paths against the isolated baseline. Observe actual job completion and step results rather than fixed sleeps. Preserve rejected records and reconcile multi-file changes before rerunning failed posting (`app/jcl/POSTTRAN.jcl`, `app/cbl/CBTRN02C.cbl`).

Record source revision, changed members, compiler/runtime context, checks run, outcomes and checks not run. Use [testing](testing.md), [debugging](debugging.md) and [deployment](../deployment.md) for the relevant verification boundaries, then follow the [contribution guide](index.md).
