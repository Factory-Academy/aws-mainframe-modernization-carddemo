# Data models

[Reference](index.md) · [Configuration](configuration.md) · [Core features](../features/core/index.md)

## Core fixed records

Lengths include filler and assume the source's host DISPLAY conventions: one byte per character/digit and no separate sign byte. `V` is an implied decimal point, not a stored character. IDCAMS `KEYS(length offset)` uses a **zero-based offset**; key positions below are **one-based**.

| Record | Bytes | Primary key: positions, length | Authoritative layout and definition |
| --- | ---: | --- | --- |
| Account | 300 | 1–11, 11 display digits | `app/cpy/CVACT01Y.cpy`; `app/jcl/ACCTFILE.jcl` |
| Customer | 500 | 1–9, 9 display digits | `app/cpy/CVCUS01Y.cpy`; `app/jcl/CUSTFILE.jcl` |
| Card | 150 | 1–16, 16 characters | `app/cpy/CVACT02Y.cpy`; `app/jcl/CARDFILE.jcl` |
| Card cross-reference | 50 | 1–16, 16-character card number | `app/cpy/CVACT03Y.cpy`; `app/jcl/XREFFILE.jcl` |
| User security | 80 | 1–8, 8-character user ID | `app/cpy/CSUSR01Y.cpy`; `app/jcl/DUSRSECJ.jcl` |
| Transaction | 350 | 1–16, 16-character transaction ID | `app/cpy/CVTRA05Y.cpy`; `app/jcl/TRANFILE.jcl` |
| Daily transaction input | 350 | Sequential input; leading 16-character transaction ID | `app/cpy/CVTRA06Y.cpy`; `app/cbl/CBTRN02C.cbl` |
| Transaction-category balance | 50 | 1–17: account 11 + type 2 + category 4 | `app/cpy/CVTRA01Y.cpy`; `app/jcl/TCATBALF.jcl` |
| Disclosure group | 50 | 1–16: group 10 + type 2 + category 4 | `app/cpy/CVTRA02Y.cpy`; `app/jcl/DISCGRP.jcl` |
| Transaction type | 60 | 1–2, 2-character type | `app/cpy/CVTRA03Y.cpy`; `app/jcl/TRANTYPE.jcl` |
| Transaction category | 60 | 1–6: type 2 + category 4 | `app/cpy/CVTRA04Y.cpy`; `app/jcl/TRANCATG.jcl` |
| Statement transaction projection | 350 | 1–32: card 16 + transaction ID 16 | `app/cpy/COSTM01.CPY`; `app/jcl/CREASTMT.JCL` |

The canonical customer load uses `KEYS(9 0)` in `app/jcl/CUSTFILE.jcl`. The separate example `app/jcl/DEFCUST.jcl` uses `KEYS(10 0)` and must not be substituted blindly. Similarly, `app/cpy/CUSTREC.cpy` is a separate customer declaration with a differently named birth-date field; the label alone does not define a new persisted format.

### Alternate access paths

| Base record | Alternate key | Definition and behavior |
| --- | --- | --- |
| Card | Account ID at positions 17–27; `KEYS(11 16)` | `app/jcl/CARDFILE.jcl`: nonunique, upgraded AIX; CICS path `CARDAIX`. |
| Cross-reference | Account ID at positions 26–36; `KEYS(11 25)` | `app/jcl/XREFFILE.jcl`: nonunique, upgraded AIX; CICS path `CXACAIX`. |
| Transaction | Processing timestamp at positions 305–330; `KEYS(26 304)` | `app/jcl/TRANFILE.jcl`, `app/jcl/TRANIDX.jcl`: nonunique AIX. Base CSD does not define a corresponding file resource. |

An AIX/path returns base records through another key; it is not a new business record. Account-keyed card/cross-reference access can yield multiple records.

### Relationships and fields

The cross-reference stores card number, customer ID and account ID, bridging the three masters. Card embeds its account ID; transactions carry a card number rather than a direct account ID. These are application joins, not VSAM-enforced foreign keys.

Account balance/limits/cycle amounts are `PIC S9(10)V99` DISPLAY; transaction and category-balance amounts use `PIC S9(9)V99`; disclosure rates use `PIC S9(4)V99`. Signed DISPLAY is not ordinary unsigned decimal text: decoding must respect sign/overpunch and host character encoding. Account dates are ten-character fields; transaction timestamps are 26-character fields, not native SQL date types. Source spelling such as `ACCT-EXPIRAION-DATE` is part of the interface.

**The actual core account ID is 11 display digits.** The 16-character account mapping in `.factory/skills/cobol-to-java-conversion/mappings/vsam-to-sql.md` and 16-digit IDs from `.factory/skills/cobol-to-java-conversion/scripts/generate-test-data.py` are illustrative migration material, not a replacement layout. The Java templates are not copybook-authoritative. Card numbers and transaction IDs being 16 characters does not change account-key width.

Security records contain a fixed eight-character password field; card/customer records contain sensitive identifiers and financial/contact attributes. This describes fields only, not fixture values. See [security](../security.md) before copying data to a new system.

## Optional Db2 schemas

Three table definitions appear across six DDL files, including their indexes. They are optional contracts, not a relational schema for the entire application.

| Table | Key and payload contract | Source |
| --- | --- | --- |
| `CARDDEMO.TRANSACTION_TYPE` | `TR_TYPE CHAR(2)` primary key; non-null `TR_DESCRIPTION VARCHAR(50)`. | `app/app-transaction-type-db2/ddl/TRNTYPE.ddl` |
| `CARDDEMO.TRANSACTION_TYPE_CATEGORY` | `(TRC_TYPE_CODE CHAR(2), TRC_TYPE_CATEGORY CHAR(4))` primary key; non-null `TRC_CAT_DATA VARCHAR(50)`; type FK with `ON DELETE RESTRICT`. | `app/app-transaction-type-db2/ddl/TRNTYCAT.ddl` |
| `CARDDEMO.AUTHFRDS` | `(CARD_NUM CHAR(16), AUTH_TS TIMESTAMP)` primary key; request/decision/merchant fields; amounts `DECIMAL(12,2)`, POS mode `SMALLINT`, merchant name `VARCHAR(22)`, report date `DATE`, account/customer IDs `DECIMAL(11)`/`DECIMAL(9)`. | `app/app-authorization-ims-db2-mq/ddl/AUTHFRDS.ddl` |

Index sources are `app/app-transaction-type-db2/ddl/XTRNTYPE.ddl`, `app/app-transaction-type-db2/ddl/XTRNTYCAT.ddl`, and `app/app-authorization-ims-db2-mq/ddl/XAUTHFRD.ddl`. The fraud index sorts card ascending and authorization timestamp descending.

Db2 varying-character host structures have a binary length plus text: preserve this in `app/app-transaction-type-db2/dcl/DCLTRTYP.dcl`, `app/app-transaction-type-db2/dcl/DCLTRCAT.dcl`, and `app/app-authorization-ims-db2-mq/dcl/AUTHFRDS.dcl`. SQL nullability does not by itself prove that every embedded-SQL path handles null indicators.

`app/app-transaction-type-db2/jcl/TRANEXTR.jcl` pads descriptions to 50 characters and appends filler to export **60-byte** type/category records. Extracting does not automatically refresh VSAM; separate load jobs are required. See [transaction types](../systems/transaction-types.md). Fraud timestamp construction and insert/update semantics are explained in [fraud marking](../systems/authorizations/online-and-fraud.md#db2-fraud-action); the stored timestamp is not simply the incoming MQ date/time string.

## IMS hierarchy and movement formats

`app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd` defines a VSAM HIDAM hierarchy, with primary index in `app/app-authorization-ims-db2-mq/ims/DBPAUTX0.dbd`.

| Segment | Bytes | Unique sequence field | Layout |
| --- | ---: | --- | --- |
| `PAUTSUM0` root | 100 | `ACCNTID`, first 6 bytes, packed signed 11-digit account ID | `app/app-authorization-ims-db2-mq/cpy/CIPAUSMY.cpy` |
| `PAUTDTL1` child | 200 | `PAUT9CTS`, first 8 bytes, DBD character key | `app/app-authorization-ims-db2-mq/cpy/CIPAUDTY.cpy` |

The child key combines packed `S9(5)` reverse date and `S9(9)` reverse time (3 + 5 bytes). Processor construction subtracts YYDDD from 99999 and HHMMSSmmm from 999999999. It is a processing-order key, not a transaction-ID deduplication contract. Segment fields mix character, DISPLAY, binary `COMP` and packed `COMP-3`. Summary counters use binary storage; monetary fields use packed decimal.

`app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb` exposes both segments with `PROCOPT=AP`, `KEYLEN=14`, `CMPAT=YES`. Online database PCB is 1; compatible batch includes an I/O PCB before the database PCB. Other load/unload PSBs and PCB order are documented in [IMS utilities](../systems/authorizations/batch-utilities.md#abstractions-dbds-psbs-and-pcbs).

| Movement contract | Summary | Detail | Source |
| --- | --- | --- | --- |
| Application sequential export/reload | 100 bytes | **206 bytes: 6-byte parent key + 200-byte child** | `app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL`, `app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL`, `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL` |
| GSAM export | 100 bytes | **200-byte raw child**, no parent prefix | `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL`, `app/app-authorization-ims-db2-mq/ims/PASFLDBD.DBD`, `app/app-authorization-ims-db2-mq/ims/PADFLDBD.DBD` |
| Native IMS utility unload | Utility framing, not application fixed records | JCL declares VB, LRECL 27990 | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl` |

These formats are not interchangeable. The source provides no matching GSAM or native-utility reload job in this subtree; the sequential loader inserts and tolerates duplicates rather than overwriting existing segments. See [movement caveats](../systems/authorizations/batch-utilities.md).

## Storage, encoding and change rules

- ASCII/EBCDIC directories label supplied fixtures, not a universal conversion strategy. Do not infer CCSID, RDW/block framing or valid host records solely from an extension.
- Text conversion is unsafe for raw IMS images containing `COMP`/`COMP-3`. Binary width, endianness, packed sign nibble, DISPLAY sign conventions and padding must be explicitly mapped during migration.
- Fixed records retain filler; stripping trailing spaces changes lengths. VB files add framing distinct from business data. Report projections and edited currency are not master records.
- COMMAREA `app/cpy/COCOM01Y.cpy` and BMS symbolic maps are task/UI contracts, not persisted tables. Extend compatible callers together.
- Change copybook, FD, key/length declarations, CSD, SQL/DCL, sort offsets and load/export programs as a coordinated change. Verify byte-preserving round trips and boundary values on the target runtime; no local host integration suite is supplied.

MQ is a separate wire layer: see [inquiry requests/replies](../systems/mq-inquiries.md#actual-message-contracts) and [authorization request/reply parsing](../systems/authorizations/mq-processing.md#request-contract). Neither protocol is a direct serialization of these master records.
