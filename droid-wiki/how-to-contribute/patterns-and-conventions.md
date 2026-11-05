# Patterns and conventions

CardDemo deliberately contains different mainframe coding styles. Preserve a program's existing layout and host contracts rather than applying a repository-wide stylistic rewrite (`README.md`, `CONTRIBUTING.md`).

## Source format and shared contracts

Some files have sequence numbers and trailing identification columns, while others leave those columns blank. For example, `app/cpy/COADM02Y.cpy` contains numbered source and `app/cpy/COCOM01Y.cpy` uses unnumbered fixed-format lines. Treat columns, continuation indicators, padding, and compiler source format as part of the build contract.

Persistent records live in copybooks such as `app/cpy/CVACT01Y.cpy`. The account record is 300 bytes and includes signed decimal amounts. A field rename, width change, or representation change can affect COBOL programs, fixtures, VSAM keys, JCL record lengths, and external MQ consumers. Do not infer byte lengths from an equivalent-looking Java or SQL type. Consult [Data models](../reference/data-models.md).

BMS source and symbolic map copybooks are paired: `app/bms/COSGN00.bms` defines the map and `app/cpy-bms/COSGN00.CPY` defines COBOL fields. Rebuild the map artifacts together when altering screens.

## CICS flow

`app/cbl/COSGN00C.cbl` and `app/cbl/COMEN01C.cbl` use `EIBCALEN`, `EIBAID`, and COMMAREA state to distinguish initial display from reentry. They send/receive maps, inspect input, and use `XCTL` or `RETURN TRANSID` to continue the user journey.

`app/cpy/COCOM01Y.cpy` centralizes user type, program context, navigation, and selected customer/account/card. Its 88-level conditions name states such as administrator and reentry. Keep those meanings consistent across callers; do not replace shared context without tracing every consumer.

Menu labels and program names are data, not just screen literals. Regular options are in `app/cpy/COMEN02Y.cpy`; administrator options are in `app/cpy/COADM02Y.cpy`. Optional program availability is checked by dispatch logic, so adding a menu entry is not equivalent to installing its runtime resources (`app/cbl/COMEN01C.cbl`).

## Error handling

| Boundary | Pattern | Example |
| --- | --- | --- |
| CICS file/map calls | `RESP` and `RESP2`, followed by explicit result handling | `app/cbl/COSGN00C.cbl` |
| Online validation | 88-level conditions, field-specific messages, cursor placement | `app/cbl/COACTVWC.cbl` |
| Batch I/O | `FILE STATUS`, end-of-file state, application result codes | `app/cbl/CBACT04C.cbl` |
| Db2 | Embedded SQL, host declarations, SQLCA/status checks | `app/app-transaction-type-db2/cbl/COBTUPDT.cbl` |
| MQ | MQ completion and reason codes around queue operations | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl` |

These are local patterns, not a common exception framework. Trace error paths in the specific program before changing behavior.

## Encoding and data safety

`app/data/ASCII/` and `app/data/EBCDIC/` are different fixture representations. Binary transfer is required for the supplied EBCDIC data (`README.md`). Signed display fields and packed/binary values require representation-aware decoding; ordinary text conversion is not a universal migration strategy.

Use synthetic fixtures and redact records in diagnostics. The demonstration signon compares application security-file fields directly; it is not a production password system (`app/cbl/COSGN00C.cbl`). See [Security](../security.md).

## Change boundaries

Keep each change focused, as required by `CONTRIBUTING.md`. For a screen change, trace its BMS map, symbolic map, COBOL program, COMMAREA, and CICS resources. For a batch change, trace copybooks, `SELECT`/DD bindings, record formats, upstream jobs, and downstream reports. [Testing](testing.md) describes verification when a host is available; there is no checked-in general-purpose test runner to substitute for host checks.
