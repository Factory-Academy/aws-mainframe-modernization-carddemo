# IMS views and Db2 fraud marking

Active contributors: Arunkumar

[Authorizations](index.md) · [MQ producer path](mq-processing.md)

## Purpose

The CICS summary and detail screens browse IMS authorization history and let an operator mark or remove a fraud designation. A linked program mirrors the designation into Db2. This is manual reporting, not an automated fraud scoring engine. [Subsystem statistics](index.md#bounded-statistics) cover these files; there are no dedicated automated tests in the subtree.

## Layout and abstractions

| Runtime entry | Program and map |
| --- | --- |
| `CPVS` | `COPAUS0C`, map `COPAU0A` in mapset `COPAU00` |
| `CPVD` | `COPAUS1C`, map `COPAU1A` in mapset `COPAU01` |
| CICS `LINK` from detail | `COPAUS2C`, no terminal map |

Both screens extend the shared `app/cpy/COCOM01Y.cpy` communication area with matching selection/pagination fields: selected eight-byte IMS child key, five visible child keys, page number, last key, next-page flag and twenty saved page keys. Changes must preserve their compatible offsets; the detail program appends a further fraud-data area.

### IMS schema

`app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd` defines a VSAM-backed HIDAM database:

| Segment | Bytes | Key |
| --- | --- | --- |
| `PAUTSUM0`, root | 100 | Unique `ACCNTID`, first 6 bytes, packed account ID |
| `PAUTDTL1`, child | 200 | Unique `PAUT9CTS`, first 8 bytes |

The root contains customer ID, status fields, limits/balances and approved/declined counts and amounts. The child key is two packed reverse date/time fields, although the DBD treats their combined sequence field as character bytes. Remaining child fields carry original request date/time, card and merchant attributes, decision, amounts, transaction ID, match status and fraud status/date.

Match values are `P` pending, `D` declined, `E` pending-expired and `M` matched. Fraud values are `F` confirmed and `R` removed. Declaring these values does not establish a transaction-matching workflow; the processor assigns `P`/`D`, while the purge deletes records rather than changing them to `E`.

`app/app-authorization-ims-db2-mq/ims/DBPAUTX0.dbd` defines the HIDAM primary index with a six-byte packed key pointing back to the root. It is not a second independent authorization store.

## Summary and detail flow

1. `COPAUS0C` accepts a numeric account ID. `GETCARDXREF-BYACCT` reads core `CXACAIX`, then account/customer reads use `ACCTDAT` and `CUSTDAT`. Header information comes from VSAM, while authorization counts/balances come from IMS.
2. `GET-AUTH-SUMMARY` schedules `PSBPAUTB`, then issues a root `GU` on `ACCNTID`. `GET-AUTHORIZATIONS` browses child records with `GNP`.
3. The summary displays five rows. PF7/PF8 use saved keys and qualified child positioning; an additional read probes for a next page. The saved-page-key array has twenty entries, so changes or tests should explicitly address histories beyond that bound rather than assuming unlimited paging.
4. Selecting `S`/`s` and Enter passes the child key through the communication area with `XCTL` to `COPAUS1C`. PF3 returns toward the core menu.
5. `COPAUS1C` repositions to the account root, then retrieves the selected child with `GNP WHERE PAUT9CTS = key`. PF8 fetches the next child; PF3 returns to summary. PF5 invokes fraud marking/removal.

The BMS footer explicitly labels PF5 “Mark/Remove Fraud.” The detail reason-description table includes daily-limit and lost-card text beyond the processor's implemented rules; display text is not evidence those checks execute.

## Db2 fraud action

```mermaid
sequenceDiagram
    participant U as Detail screen COPAUS1C
    participant I as IMS
    participant F as Linked COPAUS2C
    participant D as Db2 AUTHFRDS
    U->>I: Read root and selected child
    U->>U: Toggle F / R in child buffer
    U->>F: LINK account, customer, 200-byte detail, action/status
    F->>D: INSERT; on SQLCODE -803 UPDATE flag/date
    F-->>U: Success/failure and updated detail buffer
    alt SQL and IMS replacement succeed
      U->>I: REPL detail
      U->>U: CICS SYNCPOINT
    else Link, SQL, or replacement fails
      U->>U: CICS SYNCPOINT ROLLBACK
    end
```

`COPAUS2C` constructs a timestamp using the original authorization date and the decoded **processing-time** key's time/milliseconds. It does not simply use the incoming original HHMMSS field. This distinction matters when comparing Db2 keys to MQ payloads.

The table `CARDDEMO.AUTHFRDS` has primary key `(CARD_NUM, AUTH_TS)`. It stores request/decision/merchant data, decimal amounts, account/customer IDs, match status, fraud flag and report date. `MERCHANT_NAME` is `VARCHAR(22)` with a length/text host structure; POS entry mode is `SMALLINT`; amounts are `DECIMAL(12,2)`. The unique index orders card ascending and timestamp descending.

The writer initially inserts all mapped fields with `FRAUD_RPT_DATE = CURRENT DATE`. SQLCODE `-803` invokes `FRAUD-UPDATE`, which changes only fraud action and report date for the card/timestamp key. Removing fraud writes `R`; it does not delete the row. SQL failures return an application status and SQLCODE/SQLSTATE message. The linked program also sets the IMS detail's eight-character report-date field in MM/DD/YY form; Db2 stores a native date.

### Syncpoints and error boundaries

`COPAUS0C` takes a syncpoint before sending a screen when its PSB-scheduled flag is set. Normal detail reads/navigation also take a syncpoint. For fraud, `COPAUS1C` links the SQL writer in the same CICS task, attempts IMS `REPL` only after a successful link/application status, and takes a syncpoint on successful replacement. Link/SQL/replacement failures request `SYNCPOINT ROLLBACK`.

This is source evidence for CICS-managed coordination **intent**, not proof of a tested two-phase-commit guarantee. The code does not check syncpoint responses; deployed IMS/Db2 attachment, recovery and plan configuration are prerequisites. Read-and-replace paths use the supplied EXEC DLI forms; do not invent extra explicit locking or retry guarantees. Some read errors send an error screen without a hard stop, so error text is not itself proof that all subsequent processing was prevented.

Unlike the [MQ path](mq-processing.md#reply-contract-and-delivery-boundary), the fraud action has explicit rollback statements. It still has no checked-in failure-injection or recovery test proving cross-store outcomes.

## Integration and modification entry points

- `app/app-authorization-ims-db2-mq/csd/CRDDEMO2.csd` defines the mapsets, programs, transactions, sample Db2 entry/plan and `CPVD` association. Its `RESSEC(NO)`/`CMDSEC(NO)` settings are demo configuration, not evidence of production authorization controls.
- `app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb` provides database access `PROCOPT=AP`, both segments, `KEYLEN=14`, and `CMPAT=YES`. Online code selects database PCB 1. The load-only PSB is not the one these screens schedule.
- Keep DDL, DCL, embedded SQL and target schema aligned. The README's schema/plan placeholders and examples are not the same as the executable source's fixed names.
- For a new screen field, update the BMS source, matching symbolic map and COBOL populate/receive logic. For a fraud field, also update the 200-byte detail layout, link buffer, SQL mapping and schema. A layout change affects the [batch formats](batch-utilities.md).

## Key source files

| Path | Role |
| --- | --- |
| `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl` | Summary, VSAM joins and paging |
| `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl` | Detail retrieval, link protocol, commit/rollback |
| `app/app-authorization-ims-db2-mq/cbl/COPAUS2C.cbl` | SQL and timestamp mapping |
| `app/app-authorization-ims-db2-mq/bms/COPAU00.bms` | Summary map |
| `app/app-authorization-ims-db2-mq/bms/COPAU01.bms` | Detail map |
| `app/app-authorization-ims-db2-mq/cpy-bms/COPAU00.cpy` | Summary symbolic layout |
| `app/app-authorization-ims-db2-mq/cpy-bms/COPAU01.cpy` | Detail symbolic layout |
| `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd` | Primary hierarchy and lengths |
| `app/app-authorization-ims-db2-mq/ims/DBPAUTX0.dbd` | Primary index |
| `app/app-authorization-ims-db2-mq/ddl/AUTHFRDS.ddl` | Fraud table |
| `app/app-authorization-ims-db2-mq/ddl/XAUTHFRD.ddl` | Unique index |
| `app/app-authorization-ims-db2-mq/dcl/AUTHFRDS.dcl` | Host variables |
| `app/app-authorization-ims-db2-mq/csd/CRDDEMO2.csd` | Runtime resource associations |

## Related links

[MQ decisions](mq-processing.md) · [Database utilities](batch-utilities.md) · [Shared conventions](../../how-to-contribute/patterns-and-conventions.md) · [Configuration](../../reference/configuration.md)
