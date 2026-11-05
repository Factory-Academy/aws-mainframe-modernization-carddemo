# Glossary

This glossary explains the host terminology used by CardDemo and its optional extensions. Start with [Architecture](architecture.md) for the execution model.

| Term | Meaning in this repository | Source |
| --- | --- | --- |
| CICS | Transaction runtime for online COBOL programs and terminal interactions | `app/cbl/COSGN00C.cbl` |
| BMS / mapset | Screen definitions and their compiled terminal-map resources | `app/bms/COSGN00.bms`, `app/cpy-bms/COSGN00.CPY` |
| COMMAREA | Bytes passed between CICS tasks/programs; CardDemo stores identity and navigation context here | `app/cpy/COCOM01Y.cpy` |
| Pseudo-conversation | A screen interaction ends with `RETURN TRANSID`; the next terminal input starts another task with saved context | `app/cbl/COMEN01C.cbl` |
| EIBCALEN / EIBAID | CICS communication-area length and terminal attention key | `app/cbl/COSGN00C.cbl` |
| XCTL / LINK | Transfer control to another CICS program, or invoke a program that returns to the caller | `app/cbl/COSGN00C.cbl`, `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl` |
| VSAM / KSDS | Indexed record storage used for account, card, customer, cross-reference, and transaction data | `app/csd/CARDDEMO.CSD` |
| AIX / PATH | Alternate index and access path, such as account-based access into a card-keyed file | `app/jcl/XREFFILE.jcl`, `app/csd/CARDDEMO.CSD` |
| Copybook | COBOL text included with `COPY`, often a persistent record or shared context layout | `app/cpy/CVACT01Y.cpy` |
| PIC / COMP / COMP-3 | COBOL layout and representation clauses; binary and packed-decimal fields must retain their format during migration | `.factory/skills/cobol-to-java-conversion/mappings/data-types.md` |
| HLQ | High-level qualifier at the start of a mainframe dataset name | `app/jcl/POSTTRAN.jcl` |
| JCL / DD | Job Control Language and data-definition statements binding programs to datasets | `app/jcl/POSTTRAN.jcl` |
| JES / SYSOUT | Job entry system and job output streams used by submission helpers and batch diagnostics | `scripts/remote_submit.sh`, `app/jcl/POSTTRAN.jcl` |
| PROC | Reusable JCL procedure | `samples/proc/BUILDBAT.prc` |
| GDG | Generation data group for versioned datasets, referenced with relative generations | `app/jcl/DEFGDGB.jcl`, `app/jcl/POSTTRAN.jcl` |
| RACF | Host security administration, separate from CardDemo's application user file | `samples/jcl/RACFCMDS.jcl`, `app/cpy/CSUSR01Y.cpy` |
| Db2 / SQLCA | Optional relational database and SQL status area | `app/app-transaction-type-db2/cbl/COBTUPDT.cbl` |
| IMS / HIDAM | Hierarchical database runtime and indexed access method used for pending authorizations | `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd` |
| DBD / PSB | IMS database definition and program specification defining accessible database views | `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd`, `app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb` |
| MQ | IBM message queues used by optional authorization and inquiry programs | `app/app-vsam-mq/cbl/COACCT01.cbl` |
| Disclosure group | Reference data used with account and category balances for interest processing | `app/cpy/CVTRA02Y.cpy`, `app/cbl/CBACT04C.cbl` |
| Pending authorization | Authorization stored in IMS and exposed through optional summary/detail screens | `app/app-authorization-ims-db2-mq/README.md` |

Record sizes and keys are listed in [Data models](../reference/data-models.md). Resource naming is covered in [Configuration](../reference/configuration.md).
