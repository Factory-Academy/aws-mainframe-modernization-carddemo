# Testing

No checked-in CI workflow, Makefile or general unit-test harness supplies an application-wide test command. Sample JCL, diagnostic readers and modernization checklists are useful inputs to verification, but they are not an automated regression suite (`samples/jcl/REPRTEST.jcl`, `samples/jcl/SORTTEST.jcl`, `.factory/skills/cobol-to-java-conversion/checklists/post-conversion-validation.md`).

## Safe local checks

From the repository root, `git diff --check` checks whitespace errors in tracked changes; it does not compile COBOL or validate host behavior. Inspect fixed-format columns, continuation rules, source/member case, COPY dependencies, map layouts and resource names against [patterns and conventions](patterns-and-conventions.md). For new untracked documentation, separately validate its links and source paths because ordinary `git diff` does not include it.

Do not use `scripts/local_compile.sh` as a regression command: it targets an absent sizing example. `scripts/remote_compile.sh` depends on a missing Makefile and submits host JCL; neither establishes a working local CardDemo build. The modernization analyzer is lexical and its synthetic generator is not a deterministic, copybook-compatible fixture exporter; see [modernization](../background/modernization.md).

## Validate fixtures without publishing records

- Determine whether the artifact is text source, fixed host records or utility-unload data before choosing transfer mode. `README.md` requires binary transfer for EBCDIC fixtures; the actual core path is `app/data/EBCDIC/`, not the stale path in the README.
- Check record length and framing against the copybook and allocation JCL. Core examples include account 300 bytes, card 150, customer 500, cross-reference 50 and transaction 350 (`app/cpy/CVACT01Y.cpy`, `app/cpy/CVACT02Y.cpy`, `app/cpy/CVCUS01Y.cpy`, `app/cpy/CVACT03Y.cpy`, `app/cpy/CVTRA05Y.cpy`).
- For a confirmed headerless, delimiter-free fixed-record artifact, byte size divisible by record length is a useful structural check, not proof that keys, signs, values or encoding are valid. Do not apply that assumption to newline-delimited ASCII or VB/RDW utility data.
- Check keys, cross-reference relationships, signed-display and packed fields after the intended conversion. Preserve leading zeros and decimal scale; do not decode every byte as ordinary text.
- IMS custom sequential export uses 100-byte roots and 206-byte keyed children; GSAM children are 200 bytes, while native utility unload has a separate VB contract (`app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL`, `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL`, `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl`).

Use synthetic identities and redact fixture/output contents. Capture only structural counts, sanitized statuses and expected/actual results in public review material.

## Proposed host verification matrix

These checks require a provisioned isolated runtime and are **not** a checked-in or executed test suite.

| Boundary | Cases and evidence |
| --- | --- |
| Access and menus | Unknown/invalid sign-on, regular/admin routing, no-COMMAREA entry, invalid options, unavailable optional target; inspect `app/cbl/COSGN00C.cbl` and `app/cbl/COMEN01C.cbl`. |
| Accounts/cards | Missing and inconsistent keys, first/last pages, invalid dates/status, confirmation/cancel and concurrent edit; inspect `app/cbl/COACTUPC.cbl` and `app/cbl/COCRDUPC.cbl`. |
| Entry/payment | Empty/nonnumeric last transaction key, concurrent ID allocation, full-balance payment and failure between writes; inspect `app/cbl/COTRN02C.cbl` and `app/cbl/COBIL00C.cbl`. |
| Posting/interest | Reject reasons 100–103, duplicate IDs, partial multi-file updates, rate fallback, empty input and final-account handling; inspect `app/cbl/CBTRN02C.cbl` and `app/cbl/CBACT04C.cbl`. |
| Reports/statements | Inclusive date bounds, reference descriptions, account/grand totals and statement-table limits; inspect `app/cbl/CBTRN03C.cbl` and `app/cbl/CBSTM03A.CBL`. |
| Db2 types | Missing/duplicate type, dependent-category deletion, filter/page boundaries and 60-byte exports followed by separate VSAM load; inspect `app/app-transaction-type-db2/cbl/COTRTUPC.cbl` and `app/app-transaction-type-db2/jcl/TRANEXTR.jcl`. |
| MQ inquiries | `INQA` plus an 11-digit account key, malformed/short consecutive messages, fixed reply routing, IDs, CCSID/sign decoding and queue failures; inspect `app/app-vsam-mq/cbl/COACCT01.cbl`. |
| MQ authorizations | Multi-message framing, decline accounting, malformed CSV, consumed request/reply with failed IMS persistence; inspect `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`. |
| Fraud action | Link/SQL/IMS replacement failures and actual cross-store outcome after commit/rollback; inspect `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl`. Syncpoint intent is not verified two-phase commit. |
| Purge | Year rollover, mixed ages, all-declined roots, surviving-root totals and credit balance; inspect `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`. |
| Utilities | Reused date buffers, invalid selectors/leap dates and centisecond wait input; inspect `app/asm/COBDATFT.asm`, `app/cbl/CSUTLDTC.cbl`, `app/cbl/COBSWAIT.cbl`. |

The [purge caveats](../systems/authorizations/batch-utilities.md#purge-flow-and-important-discrepancies) include a duplicated approved-count condition, noncalendar age subtraction, no surviving-root REPL and no credit release. Tests must expose those source behaviors rather than assume the README's intended results.

## Acceptance evidence

Record initial fixture/snapshot identity, source revision, installed members, job/step completion, sanitized response codes, rejects and before/after balances/counts. A submission acknowledgment, elapsed sleep or RC 0 alone does not prove business correctness: posting sets RC 4 for rejects, and some IDCAMS jobs normalize return codes (`app/cbl/CBTRN02C.cbl`, `app/jcl/TRANBKP.jcl`).

Reconcile partial effects before restart; do not blindly rerun input or refresh datasets to conceal a failure. Continue with [debugging](debugging.md), [batch processing](../features/core/batch-processing.md) and [deployment](../deployment.md).
