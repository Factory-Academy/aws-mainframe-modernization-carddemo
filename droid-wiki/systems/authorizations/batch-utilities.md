# Purge and database utilities

Active contributors: Arunkumar

[Authorizations](index.md) · [IMS schema and screens](online-and-fraud.md)

## Purpose

These host-only programs purge old IMS authorizations and move database contents through sequential files, GSAM or an IMS utility unload. They are separate format contracts, not interchangeable backups or automated tests. The [subsystem inventory](index.md#bounded-statistics) gives bounded file/contributor/test statistics.

## Layout and entry points

| Job | Program / PSB | Effect |
| --- | --- | --- |
| `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl` | BMP `CBPAUP0C` / `PSBPAUTB` | Deletes authorizations by age |
| `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL` | DLI `PAUDBUNL` / `PAUTBUNL` | Deletes previous output datasets, then creates fixed sequential exports |
| `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL` | BMP `PAUDBLOD` / `PSBPAUTB` | Inserts exported roots and children |
| `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL` | DLI `DBUNLDGS` / `DLIGSAMP` | Inserts raw segment images into existing GSAM outputs |
| `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl` | ULU `DFSURGU0` / database `DBPAUTP0` | Deletes previous utility output, then produces IMS database unload |

The JCL contains installation-specific IMS libraries, database datasets and buffer-pool member names. The purge job also contains an unresolved production-load-library placeholder. These jobs can modify or delete data; do not submit them merely to validate documentation.

## Abstractions: DBDs, PSBs and PCBs

| Definition | Contract |
| --- | --- |
| `app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb` | Database `DBPAUTP0`, `PROCOPT=AP`, `KEYLEN=14`, `CMPAT=YES`, both root and child visible |
| `app/app-authorization-ims-db2-mq/ims/PSBPAUTL.psb` | Load-only `PROCOPT=L`, `LANG=ASSEM`; supplied definition, not the PSB selected by `LOADPADB.JCL` |
| `app/app-authorization-ims-db2-mq/ims/PAUTBUNL.PSB` | Read-oriented `PROCOPT=GOTP`, `CMPAT=NO`, used by sequential unload |
| `app/app-authorization-ims-db2-mq/ims/DLIGSAMP.PSB` | Same database read PCB plus two GSAM PCBs with `PROCOPT=LS`, `CMPAT=NO` |
| `app/app-authorization-ims-db2-mq/ims/PASFLDBD.DBD` | GSAM/BSAM fixed 100-byte summary records; DDs `PASFILIP` / `PASFILOP` |
| `app/app-authorization-ims-db2-mq/ims/PADFLDBD.DBD` | GSAM/BSAM fixed 200-byte detail records; DDs `PADFILIP` / `PADFILOP` |

The `CMPAT=YES` batch environment includes an I/O PCB before the database PCB: purge EXEC DLI uses **PCB 2**, whereas online code uses **PCB 1**. Loader linkage accepts I/O then database PCB. Sequential/GSAM unload linkage matches the `CMPAT=NO` PSBs. Preserve PCB order when changing these definitions.

The three PCB masks and function constants live in `app/app-authorization-ims-db2-mq/cpy/PAUTBPCB.CPY`, `app/app-authorization-ims-db2-mq/cpy/PASFLPCB.CPY`, `app/app-authorization-ims-db2-mq/cpy/PADFLPCB.CPY`, and `app/app-authorization-ims-db2-mq/cpy/IMSFUNCS.cpy`. They expose status/key feedback and CBLTDLI operation names, not independent business records.

## Purge flow and important discrepancies

`app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl` accepts fixed-position SYSIN fields: two expiry-day digits, separator, five checkpoint-frequency characters, separator, five checkpoint-display-frequency characters, separator, debug flag and filler. Invalid expiry digits fall back to five days; blank/zero checkpoint settings have defaults. The checked-in job supplies **zero expiry days**, frequencies of one and debug on: that is not a safe retention default.

The program performs root `GN`, child `GNP`, computes age, and deletes qualifying children with `DLET`. It decrements the in-memory approved or declined count/amount according to response code. A periodic IMS `CHKP` occurs when processed-summary count is **greater than** the configured frequency, plus a final checkpoint. It emits read/delete counts and optional debugging.

Read the following behavior literally rather than repeating README claims:

- Age is calculated as subtraction of numeric **YYDDD** values after decoding the reverse date key. This is not calendar-day arithmetic across year boundaries.
- There is **no match-status or fraud-status filter**: the age test is not limited to unmatched, approved or nonfraud records.
- It does **not** reduce `PA-CREDIT-BALANCE` when deleting an authorization. The README's claim of adjusting available credit is not implemented here.
- Count/amount reductions modify the root buffer, but there is no root `REPL` to persist a surviving summary's revised totals.
- The condition before deleting a root checks `PA-APPROVED-AUTH-CNT <= 0` **twice**, not approved and declined counts. A root with remaining declined details can meet that condition; parent deletion can remove remaining children.
- The `9999-ABEND` paragraph sets return code 16 and `GOBACK`; it does not issue an explicit IMS rollback or actual abend. Checkpoint calls alone do not establish restartability; no extended-restart state restoration is supplied.

These are reasons to review and test retention in an isolated host environment before use, not instructions to run the supplied job unchanged.

## Sequential unload and reload

`PAUDBUNL` walks roots using `CBLTDLI GN` and children using `GNP`, producing:

| Output DD | Fixed record | Consumer |
| --- | --- | --- |
| `OUTFIL1` | 100-byte summary image | Loader `INFILE1` |
| `OUTFIL2` | 6-byte packed parent account key + 200-byte detail image = **206 bytes** | Loader `INFILE2` |

`UNLDPADB.JCL` declares FB lengths 100 and 206. `PAUDBLOD` reads all roots first, inserts them, then reads keyed children, qualifies `PAUTSUM0` by `ACCNTID`, retrieves the parent and inserts `PAUTDTL1`. Root and child duplicate status `II` is tolerated; this is **not an overwrite/update restore**. Non-numeric child parent keys are skipped by the source check. File and IMS failures are handled with displays and return-code paths of varying strictness; do not infer full reconciliation from a successful launch.

These programs copy raw host bytes, including packed decimal and binary fields. Text-mode conversion or treating records as comma-separated input breaks the contract. Their source contains legacy labels/counters, so operator labels are less authoritative than the actual FD layouts and DD allocations.

## GSAM unload

`DBUNLDGS` browses the same hierarchy, but `CBLTDLI ISRT` sends the **summary buffer itself** to `PASFLPCB` and the **detail buffer itself** to `PADFLPCB`. The detail stream is **200 bytes**, without the parent-key prefix used by the sequential export. Although a 206-byte staging record exists in working storage, that staging record is not the GSAM write argument.

`UNLDGSAM.JCL` uses existing output datasets with `DISP=(OLD,KEEP,KEEP)`. It does not allocate them or provide a matching GSAM reload program. Preserve ordering and establish parent association explicitly before designing a consumer; do not feed this child stream directly to the 206-byte sequential loader.

## Sample data and format boundaries

`app/app-authorization-ims-db2-mq/data/EBCDIC/AWS.M2.CARDDEMO.IMSDATA.DBPAUTP0.dat` is the supplied EBCDIC IMS-data artifact. No fixture records are reproduced here.

The matching-named utility job `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl` runs IMS `DFSURGU0`, writing `DFSURGU1` with **RECFM=VB, LRECL=27990**. This is an IMS utility-unload contract, distinct from application fixed-record exports and MQ comma-delimited text. A filename/encoding label alone does not prove that a local transferred artifact retains host RDW/block framing; verify transfer and target utility requirements before loading it. No matching utility-reload job is supplied in this subtree.

## Integration and modification entry points

For retention changes, start with `4000-CHECK-IF-EXPIRED`, the summary-delete condition, summary persistence and checkpoint/recovery design. For movement changes, compare FDs, DBD lengths, PCB order, CBLTDLI arguments and JCL DCBs together. Changing the [IMS segment schema](online-and-fraud.md#ims-schema) affects all three custom utilities and the online 200-byte fraud-link buffer.

Host-only validation should cover year rollover, all-declined roots, mixed expired/unexpired children, surviving-root totals, checkpoint boundaries, duplicate inserts and byte-preserving export/import round trips. These are proposed checks, not a checked-in or executed test suite.

## Key source files

| Path | Role |
| --- | --- |
| `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl` | Purge implementation |
| `app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL` | 100/206-byte sequential exporter |
| `app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL` | Root-first keyed reload |
| `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL` | 100/200-byte GSAM exporter |
| `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl` | Purge SYSIN and BMP launch |
| `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL` | Sequential allocation/deletion and launch |
| `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL` | Sequential reload launch |
| `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL` | GSAM output DDs |
| `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl` | Native IMS utility unload |

## Related links

[Authorization overview](index.md) · [MQ accounting](mq-processing.md#ims-accounting-and-modification-cautions) · [Deployment](../../deployment.md) · [Testing](../../how-to-contribute/testing.md) · [Data models](../../reference/data-models.md)
