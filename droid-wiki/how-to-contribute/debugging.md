# Debugging

Diagnosis starts with the failing host operation and its native response, not with a generic retry. CardDemo exposes CICS responses, COBOL file statuses, SQL diagnostics, IMS statuses, MQ codes and batch DISPLAY output; these mechanisms are not interchangeable (`app/cbl/COACTUPC.cbl`, `app/cbl/CBTRN02C.cbl`, `app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy`).

## Capture a safe reproduction

Record source revision, program/transaction or job/step, installed member version, operation, response/reason codes and a synthetic reproduction. Preserve detailed host evidence in access-controlled storage; do not paste full customer, card, security-file or queue records into issues. See [security](../security.md).

Separate transfer, compilation, installation, execution and business-result failures. An FTP success only establishes submission/transfer; a compiled load member may not be the one currently active in CICS (`scripts/remote_submit.sh`, `samples/jcl/CICCMP.jcl`).

## Symptom-led checks

| Symptom | Source-backed diagnosis |
| --- | --- |
| Sign-on fails | Distinguish missing/blank input, `USRSEC` lookup response and password mismatch in `READ-USER-SEC-FILE`. Confirm the file mapping and fixture encoding without printing credentials (`app/cbl/COSGN00C.cbl`, `app/csd/CARDDEMO.CSD`). |
| Map error or stale fields | Compare BMS, symbolic copybook and loaded map/program versions. Regenerating only a map or recompiling only a caller can leave incompatible layouts (`app/bms/COACTUP.bms`, `app/cpy-bms/COACTUP.CPY`, `samples/proc/BUILDBMS.prc`). |
| Account/card lookup fails | Record CICS operation, file, key width, RESP and RESP2; distinguish NOTFND from other failures. Check primary/alternate path definitions, file state and related records (`app/cbl/COACTVWC.cbl`, `app/csd/CARDDEMO.CSD`). |
| Account save or abend | Inspect update reads, old/current comparison, rewrite and rollback branches. `ABEND-ROUTINE` sends diagnostic data and issues ABCODE `9999`; that code alone does not identify the failed file operation (`app/cbl/COACTUPC.cbl`). |
| Batch file failure | Follow the relevant DD from `app/jcl/POSTTRAN.jcl` to its SELECT/FD and file status in `app/cbl/CBTRN02C.cbl`. Inspect allocation, key/record lengths, open state and `9910-DISPLAY-IO-STATUS`; do not treat CICS RESP as a COBOL file status. |
| Posting RC 4 | The posting mainline sets return code 4 when rejects exist. Inspect `DALYREJS` and reasons 100 missing cross-reference, 101 missing account, 102 limit exceeded, 103 origin after expiry (`app/cbl/CBTRN02C.cbl`). |
| Report acknowledged but absent | Follow the `JOBS` TDQ write, host internal-reader binding, generated JCL and JES execution. Successful WRITEQ does not mean the report job completed (`app/cbl/CORPT00C.cbl`, `app/csd/CARDDEMO.CSD`, `app/proc/TRANREPT.prc`). |
| Db2 maintenance fails | Keep SQLCODE/SQLSTATE and action together. `-532` can be a dependent-category delete; the common formatter uses DSNTIAC, which has its own return status (`app/app-transaction-type-db2/cbl/COTRTUPC.cbl`, `app/app-transaction-type-db2/cpy/CSDB2RPY.cpy`). |
| Date feedback looks odd | Distinguish character rearrangement in `app/asm/COBDATFT.asm` from LE calendar validation in `app/cbl/CSUTLDTC.cbl`. The latter's diagnostic-date copy can include a binary length prefix; online callers also special-case message 2513. |

Interpret numeric codes using the matching target-host product documentation and the exact source branch; a code stripped of its operation and subsystem is insufficient.

## MQ and authorization diagnosis

For [MQ inquiries](../systems/mq-inquiries.md), check trigger data, input queue, completion/reason codes and `CARD.DEMO.ERROR`. Account requests use `INQA` followed immediately by an eleven-digit key; replies go to fixed queues, not the incoming ReplyToQ. The README's requester/request-ID examples do not describe the executable contract (`app/app-vsam-mq/cbl/COACCT01.cbl`, `app/app-vsam-mq/cbl/CODATE01.cbl`).

For [authorization processing](../systems/authorizations/mq-processing.md), correlate the CSSL error's program, location, subsystem and codes with the VSAM read, PSB schedule, IMS operation or MQ call. Both MQ get and reply use no-syncpoint, and the reply precedes IMS writes. A received approval is not evidence that IMS persistence succeeded; blind resend is not established safe replay (`app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`).

For [fraud marking](../systems/authorizations/online-and-fraud.md), distinguish failed CICS LINK, returned SQL/application failure, IMS REPL failure and syncpoint/rollback outcome. The source expresses coordination intent, not verified two-phase commit across a deployed attachment configuration (`app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl`, `app/app-authorization-ims-db2-mq/cbl/COPAUS2C.cbl`).

For unexpected purge loss or stale totals, inspect the duplicated approved-count test, numeric YYDDD subtraction, missing surviving-summary REPL and unchanged credit balance. Do not assume the purge releases available credit or filters matched/fraud records (`app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`; [purge caveats](../systems/authorizations/batch-utilities.md)).

## Before restart

Stop dependent submissions, preserve spool/rejects/statuses, and establish which writes actually completed. Posting changes category/account records before writing the transaction master; its source does not provide an atomic multi-file restart protocol (`app/cbl/CBTRN02C.cbl`). A refresh or repeated posting can destroy evidence or repeat financial effects.

Check actual file close/open results, not wrapper sleep messages. Reconcile affected records and indexes against the baseline before an authorized restart or reopen (`app/jcl/CLOSEFIL.jcl`, `app/jcl/OPENFIL.jcl`, `app/jcl/TRANIDX.jcl`). Continue with [host monitoring](../how-to-monitor.md) and [testing](testing.md).
