# Configuration

[Reference](index.md) · [Deployment](../deployment.md) · [Data models](data-models.md)

## Configuration surfaces

CardDemo has no central environment-file or package-manager configuration. Host installation values are spread across CSD, JCL/procedures, compiler include paths, Db2 controls, IMS DBDs/PSBs and helper scripts. Checked-in names are demonstration defaults, not discovery of deployed infrastructure.

| Surface | Source and change boundary |
| --- | --- |
| CICS resources | `app/csd/CARDDEMO.CSD`: group `CARDDEMO`, files, maps, programs, transactions, libraries and internal-reader queue. |
| Batch datasets | `app/jcl/POSTTRAN.jcl`, `app/jcl/INTCALC.jcl`, `app/jcl/TRANREPT.jcl`: DD allocations, record formats, GDGs and sample dates. |
| Compile/link libraries | `samples/proc/BUILDBAT.prc`, `samples/proc/BUILDONL.prc`, `samples/proc/BUILDBMS.prc`, `samples/proc/BLDCIDB2.prc`: copybooks, compiler, translators, macros, LE and attachment libraries. |
| Scheduler | `app/scheduler/CardDemo.ca7`: example host schedule, not proof of an installed scheduler. |
| Db2 installation | `app/app-transaction-type-db2/ctl/DB2CREAT.ctl`, `app/app-transaction-type-db2/jcl/CREADB21.jcl`: database/storage/utility/bind choices. |
| IMS installation | `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd`, `app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb`: database hierarchy, DD and access contract. |
| Transfers and remote jobs | `scripts/upld_module.sh`, `scripts/remote_compile.sh`, `scripts/remote_submit.sh`: environment-specific tooling; see [tooling](../how-to-contribute/tooling.md). |

## CICS file, batch DD and dataset mapping

These are different namespaces. CICS COBOL passes the **logical file name** to `EXEC CICS READ`; batch COBOL `SELECT ... ASSIGN TO` names a **DD**; JCL/CSD resolves either to a **dataset**. A job name or loader DD is not automatically the batch program's DD.

Full dataset names below preserve checked-in demonstration casing. Replace the `AWS.M2.CARDDEMO` prefix consistently for a new installation; it is not a runtime substitution variable.

| Store | CICS file | Representative batch DD | Dataset |
| --- | --- | --- | --- |
| Account master | `ACCTDAT` | `ACCTFILE` | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` |
| Card master | `CARDDAT` | `CARDFILE` | `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS` |
| Card by account | `CARDAIX` | No corresponding allocation in the cited reader jobs | `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX.PATH` |
| Card/account/customer cross-reference | `CCXREF` | `XREFFILE`; report uses `CARDXREF` | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` |
| Cross-reference by account | `CXACAIX` | `XREFFIL1` in interest job | `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH` |
| Customer master | `CUSTDAT` | `CUSTFILE` | `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS` |
| Transaction master | `TRANSACT` | `TRANFILE` in posting | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` |
| User security | `USRSEC` | Loader utility `OUT`, not a core batch business-file DD | `AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS` |
| Category balances | Not defined in base CSD | `TCATBALF` | `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` |
| Disclosure/interest rates | Not defined in base CSD | `DISCGRP` | `AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS` |
| Transaction types | Not defined in base CSD | `TRANTYPE` | `AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS` |
| Transaction categories | Not defined in base CSD | `TRANCATG` | `AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS` |

Evidence: `app/csd/CARDDEMO.CSD`, `app/jcl/READACCT.jcl`, `app/jcl/READCARD.jcl`, `app/jcl/READCUST.jcl`, `app/jcl/READXREF.jcl`, `app/jcl/POSTTRAN.jcl`, `app/jcl/INTCALC.jcl`, `app/jcl/TRANREPT.jcl`, and `app/jcl/DUSRSECJ.jcl`. Assignments are visible in `app/cbl/CBACT01C.cbl`, `app/cbl/CBACT02C.cbl`, `app/cbl/CBCUS01C.cbl`, `app/cbl/CBACT03C.cbl`, `app/cbl/CBTRN02C.cbl`, `app/cbl/CBACT04C.cbl`, and `app/cbl/CBTRN03C.cbl`.

**DD meaning is job-specific.** In `app/jcl/INTCALC.jcl`, `TRANSACT` is a new sequential `AWS.M2.CARDDEMO.SYSTRAN(+1)` output, not the CICS transaction master. In `app/jcl/TRANREPT.jcl`, `TRANFILE` is a filtered sequential generation, not the master KSDS. Loader DDs such as `CARDDATA`/`CARDVSAM` in `app/jcl/CARDFILE.jcl` have yet another role.

### Online settings

`app/csd/CARDDEMO.CSD` defines eight file resources: six base stores and two AIX paths. The supplied settings include `RLSACCESS(NO)`, `READINTEG(UNCOMMITTED)`, `RECOVERY(NONE)` and `JOURNAL(NO)`; they are not a production recovery/consistency policy. CSD `RECORDFORMAT(V)` does not override the fixed logical layouts and IDCAMS `RECORDSIZE` values.

Sign-on is `CC00` → `COSGN00C`. Other screen routes are cataloged in [core features](../features/core/index.md). CSD queue `JOBS` is an extra-partition transient-data output with DD `INREADER` and 80-byte records; it requires a host internal-reader allocation. The authorization error path writes `CSSL`, which is not defined by the base CardDemo CSD.

## Optional resource contracts

| Module | Supplied names and unresolved installation work |
| --- | --- |
| Db2 types | `app/app-transaction-type-db2/csd/CRDDEMOD.csd` maps `CTLI`/`CTTU` to their programs and Db2 entry/plan `CARDDEMO`. DDL/SQL use schema `CARDDEMO`; adapt schema, utility controls, packages, plan and CICS attachment together. |
| Authorization | `app/app-authorization-ims-db2-mq/csd/CRDDEMO2.csd` maps `CP00`, `CPVS`, `CPVD`; its sample Db2 plan differs from the README example. DDL/SQL use `CARDDEMO.AUTHFRDS`. README schema/database/storage-group placeholders are not executable configuration. |
| IMS authorization | Primary DBD `DBPAUTP0`, index DBD `DBPAUTX0`; primary DD `DDPAUTP0`. Online access schedules `PSBPAUTB`. Actual DBD/PSB names must stay aligned with program calls, generated host definitions, DD allocations and target database datasets. |
| Authorization queues | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl` takes input queue from trigger data and output queue from inbound `ReplyToQ`. Supply queues, triggering and CICS-MQ connection externally; README queue examples are not provisioning. |
| MQ inquiries | `app/app-vsam-mq/csd/CRDDEMOM.csd` maps `CDRD`/`CDRA`. Programs use trigger-selected inputs but fixed outputs `CARD.DEMO.REPLY.DATE`, `CARD.DEMO.REPLY.ACCT`, and `CARD.DEMO.ERROR`; setting inbound `ReplyToQ` does not redirect them. |

Read [transaction-type setup](../systems/transaction-types.md#setup-caveats-and-modification-entry-points), [authorization integration](../systems/authorizations/index.md#integration-and-limits), [IMS utilities](../systems/authorizations/batch-utilities.md), and [MQ wire contracts](../systems/mq-inquiries.md#actual-message-contracts) before adapting these names. Queue-manager, subsystem, region, volume, library and buffer-pool choices are site-specific; their literal host identifiers are intentionally omitted.

## Safe adaptation checklist

1. Reconcile copybook lengths, VSAM keys, CSD paths and DD allocations; rebuild AIXes when needed.
2. Replace host libraries, dataset prefixes, region targets, subsystem/bind settings and scheduler dates together. Keep credentials out of source and generated documentation.
3. Review destructive `DELETE`/`DEFINE`/`REPRO` and old-output deletion steps before submitting any job. Shared VSAM needs a coordinated online/batch window; `DISP=SHR` alone does not make concurrent updates safe.
4. Preserve record framing and encoding on transfer, then validate on the intended host. The catalog snapshot `app/catlg/LISTCAT.txt` is historical evidence, not current configuration or a deployable manifest.
