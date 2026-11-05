# Host monitoring

The checked-in observability consists of host DISPLAY/SYSOUT output, CICS responses and screens, transient-data diagnostics and MQ error messages. No metrics exporter, distributed tracing, dashboard or alert configuration is supplied; use the actual deployed host facilities without inventing service-level guarantees (`app/cbl/CBTRN02C.cbl`, `app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy`, `app/app-vsam-mq/cbl/COACCT01.cbl`).

## Evidence by execution path

| Path | Where to inspect | What the source establishes |
| --- | --- | --- |
| Host compilation | Compiler/translator/binder listings and JES step output | Sample procedures preserve or print listings; inspect all relevant steps, not just submission (`samples/proc/BUILDBAT.prc`, `samples/proc/BUILDONL.prc`, `samples/proc/BLDCIDB2.prc`). |
| Online core | Screen errors and operation-specific RESP/RESP2 handling | Account maintenance formats errors and can send abend data before ABCODE `9999`; not every CICS error is centrally logged (`app/cbl/COACTUPC.cbl`). |
| Posting | JES output containing DISPLAY messages, counts and file statuses; `DALYREJS` output | The mainline displays processed/rejected counts and sets RC 4 for rejects (`app/cbl/CBTRN02C.cbl`, `app/jcl/POSTTRAN.jcl`). These are run summaries, not exported metrics. |
| Reports | `JOBS` TDQ submission path, then downstream JES/report output | WRITEQ success proves only that operation, not report completion (`app/cbl/CORPT00C.cbl`, `app/proc/TRANREPT.prc`). |
| Authorization MQ | CICS CSSL transient-data destination as configured by the host | Structured error record includes date/time, application, program, location, level, subsystem, two codes, message and event key (`app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`, `app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy`). |
| MQ inquiries | `CARD.DEMO.ERROR` and operation completion/reason codes | MQ failures and unexpected CICS file responses use an MQ error-message path; missing accounts can produce normal reply text (`app/app-vsam-mq/cbl/COACCT01.cbl`, `app/app-vsam-mq/cbl/CODATE01.cbl`). |
| Db2 type maintenance | SQLCODE/SQLCA and formatted screen messages | Shared DSNTIAC formatting has its own return-code handling (`app/app-transaction-type-db2/cpy/CSDB2RPY.cpy`). |
| IMS purge/movement | Job SYSOUT/DISPLAY, IMS status and checkpoint/read/delete counts where emitted | Purge debug output and summaries are not a database-consistency or restartability guarantee (`app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`, `app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL`). |

The error record's level values are `L`, `I`, `W` and `C`; subsystem values distinguish application, CICS, IMS, Db2, MQ and file errors (`app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy`). These fields help interpret an emitted record; they do not constitute a checked-in alert-routing policy.

## Operator checkpoints

Before a batch window, verify actual resource state and close-command outcomes. `app/jcl/CLOSEFIL.jcl` and `app/jcl/OPENFIL.jcl` expose command output through `ISFOUT`/`CMDOUT`, and operate on a limited file list. Do not infer that every initialization resource is closed or that all reopen commands succeeded.

For each submitted job, inspect its final state, step return codes/abends and utility messages before advancing dependencies. The shell helpers' sleep messages are not completion evidence (`scripts/run_full_batch.sh`, `scripts/run_posting.sh`). IDCAMS return-code normalization in `app/jcl/TRANBKP.jcl` is another reason to inspect utility messages and resulting datasets rather than accepting a final numeric code alone.

After posting, compare rejects, counts and actual account/category/master effects against the input baseline. Source updates span multiple files without a demonstrated atomic restart protocol (`app/cbl/CBTRN02C.cbl`). After Db2 reference maintenance, verify the separate extract and VSAM load before expecting reports to show new descriptions (`app/app-transaction-type-db2/jcl/TRANEXTR.jcl`, `app/jcl/TRANTYPE.jcl`, `app/jcl/TRANCATG.jcl`).

## Interpret absence and success carefully

- A drained MQ loop can stop on `MQRC-NO-MSG-AVAILABLE` after its five-second wait; that is not by itself an application failure (`app/app-vsam-mq/cbl/COACCT01.cbl`).
- An authorization reply can precede failed IMS persistence because get/reply use no-syncpoint; response receipt does not establish storage success (`app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`).
- Fraud code has explicit rollback paths, but source syncpoint calls do not prove deployed cross-store recovery (`app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl`).
- Purge counts do not establish correct retention or released credit: the implementation has a duplicated count condition, noncalendar arithmetic and no surviving-summary persistence (`app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`).

Retain sanitized program/location/subsystem/codes with the relevant job or task context. Keep record payloads and event keys access-controlled; [security](security.md) describes the exposure risk. Use [debugging](how-to-contribute/debugging.md) for symptom-led diagnosis and [testing](how-to-contribute/testing.md) for proposed verification rather than treating these observations as a production monitoring runbook.
