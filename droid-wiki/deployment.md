# Deployment

CardDemo installation is a host-resource and dataset exercise, not a checked-in cloud deployment pipeline. The source includes CICS/VSAM jobs, optional IMS/Db2/MQ definitions and packaged runtime samples; every environment needs its own validated configuration (`README.md`, `app/csd/CARDDEMO.CSD`, `samples/jcl/IMSMQCMP.jcl`).

## Establish the target

Use an isolated demonstration runtime with authorized COBOL compilation, CICS, VSAM, JCL/JES and required utilities. Identify source/copybook/map/procedure/load libraries, dataset prefix, job classes, storage allocation, CICS region and security permissions without copying private infrastructure values into repository documentation. Optional modules require the corresponding host products and attachment configuration; see [dependencies](reference/dependencies.md) and [configuration](reference/configuration.md).

There is no checked-in universal Makefile, CI deployment or infrastructure-as-code stack. `scripts/remote_compile.sh` references a missing Makefile and cannot establish a complete installation.

## Base installation sequence

1. **Allocate and transfer source.** `README.md` describes FB 80 source libraries for COBOL, copybooks, BMS, assembler, macros, JCL and procedures. Preserve columns and choose the intended text conversion; do not use source upload as an arbitrary binary-data path.
2. **Prepare isolated fixtures.** Transfer EBCDIC data from `app/data/EBCDIC/` in binary mode and verify target DCBs and record/key layouts against [data models](reference/data-models.md). The README's `main/-/data/EBCDIC/` path is stale. Never publish fixture credentials or records.
3. **Review initialization jobs.** Account/card/customer/cross-reference, security, transaction and reference loads can delete/recreate data (`app/jcl/ACCTFILE.jcl`, `app/jcl/CARDFILE.jcl`, `app/jcl/CUSTFILE.jcl`, `app/jcl/XREFFILE.jcl`, `app/jcl/DUSRSECJ.jcl`, `app/jcl/TRANFILE.jcl`). Review alternate indexes and GDG prerequisites, not only the primary file.
4. **Build host members.** Generate BMS maps/symbolic copybooks before compiling their callers. Adapt `samples/jcl/BMSCMP.jcl`, `samples/jcl/CICCMP.jcl`, `samples/jcl/BATCMP.jcl` and the matching procedures. Inspect translator/compiler/binder results and activate intended load members.
5. **Install current resources.** Align `app/csd/CARDDEMO.CSD` with actual datasets, programs, maps, transactions, libraries and the `JOBS` TDQ binding. `app/jcl/CBADMCDJ.jcl` is a DFHCSDUP maintenance example with duplicate/older map names and programs not present in the current core inventory; do not substitute its embedded definitions blindly for the current CSD.
6. **Verify bring-up.** Confirm file states, transaction/program/map association and read-only sign-on/inquiry navigation before updates. The entry transaction is `CC00`; use isolated demo identities, not documented fixture credentials (`app/csd/CARDDEMO.CSD`, `app/cbl/COSGN00C.cbl`).

## Optional subsystem bring-up

| Module | Additional installation boundary |
| --- | --- |
| [Db2 transaction types](systems/transaction-types.md) | DDL/DCL, adapted create/load controls, packages/plan and CSD DB2ENTRY associations. `app/app-transaction-type-db2/jcl/CREADB21.jcl` is supplied with TYPRUN=SCAN; creating definitions and installing a runnable module are different steps. Exporting Db2 rows does not reload VSAM. |
| [Authorizations](systems/authorizations/index.md) | IMS DBD/index and generated PSB resources, databases/attachments, MQ queues/trigger association, Db2 fraud table and binds, maps/programs/CSD. Start with `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd`, `app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb`, `app/app-authorization-ims-db2-mq/csd/CRDDEMO2.csd`; no external POS client or complete MQ provisioning is supplied. |
| [MQ inquiries](systems/mq-inquiries.md) | Host MQ queue-manager, queues, triggering and CICS-MQ connection plus `app/app-vsam-mq/csd/CRDDEMOM.csd`. The CSD does not define the MQ infrastructure. These programs are responders, not terminal request clients. |

SQL builds need precompile/translation/bind, and IMS/MQ builds need the matching translator and stubs (`samples/proc/BLDCIDB2.prc`, `samples/jcl/IMSMQCMP.jcl`). Do not infer attachment/recovery correctness from successful compilation.

## Batch windows and refresh hazards

Inspect the full affected resource set before closing files. `app/jcl/CLOSEFIL.jcl` and `app/jcl/OPENFIL.jcl` issue SDSF CICS commands for only `TRANSACT`, `CCXREF`, `ACCTDAT`, `CXACAIX` and `USRSEC`, not every file changed by initialization. Verify each command's actual result and any additional job-specific close/open steps.

`app/jcl/TRANBKP.jcl` copies the transaction master, then deletes/redefines its cluster and deletes its alternate index. It is not a read-only backup. `app/jcl/COMBTRAN.jcl` reloads combined transactions; `app/jcl/TRANIDX.jcl` rebuilds indexing. Confirm valid backup generations, restart decisions and index results before reopening.

The shell wrappers submit dependent jobs with fixed sleeps, not completion checks (`scripts/run_full_batch.sh`, `scripts/run_posting.sh`, `scripts/run_interest_calc.sh`, `scripts/remote_refresh.sh`). Use observed job/step completion and reconciliation before advancing. `app/scheduler/CardDemo.ca7` is an example listing whose schedule-ID distinctions matter, not evidence that any scheduler is installed. See [batch processing](features/core/batch-processing.md).

## Runtime samples and acceptance

`samples/m2/mf/CardDemo_runtime.zip` and `samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip` are packaged compatible-runtime samples. Their presence does not establish a running deployment, cloud account configuration or feature parity with every optional host product.

Keep an installation manifest and restorable baseline outside the source tree as appropriate for the host. Capture installed versions, actual compile/bind/job outcomes, file/index states and sanitized smoke-test results. No automated rollback or production recovery guarantee is supplied; source CICS file definitions include `RECOVERY(NONE)` (`app/csd/CARDDEMO.CSD`). Review [security](security.md), [testing](how-to-contribute/testing.md) and [monitoring](how-to-monitor.md) before enabling writes.
