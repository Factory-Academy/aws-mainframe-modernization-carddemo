# MQ processing, rules, and replies

Active contributors: Arunkumar

[Authorizations](index.md) · [IMS views and fraud marking](online-and-fraud.md)

## Purpose

`app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl` is the MQ-triggered CICS authorization processor (`CP00`). It makes a small credit-availability decision, sends a reply, then records eligible requests in IMS. Scope and test/contributor statistics are bounded in the [subsystem inventory](index.md#bounded-statistics).

## Layout and abstractions

The program's working storage contains a 500-byte input buffer, 200-byte output buffer, MQ descriptors/options, VSAM records, and IMS summary/detail buffers. Wire copybooks describe staging fields, not a JSON or ISO 8583 interface. Merchant-category spelling `CATAGORY` is preserved in the source contracts.

### Request contract

Paragraph `2100-EXTRACT-REQUEST-MSG` uses COBOL `UNSTRING ... DELIMITED BY ','` into these fields, in order:

| Position | Field | Staging width/type |
| --- | --- | --- |
| 1–2 | Authorization date, time | 6 characters each; consumers interpret date as YYMMDD and time as HHMMSS |
| 3–5 | Card number, authorization type, card expiry | 16, 4, 4 characters |
| 6–8 | Message type, message source, processing code | 6 each; processing code is numeric display |
| 9 | Transaction amount | 13-character temporary input converted with `FUNCTION NUMVAL` |
| 10–12 | Merchant category, acquiring country, POS entry mode | 4, 3, 2; entry mode is numeric display |
| 13–17 | Merchant ID, name, city, state, ZIP | 15, 22, 13, 2, 9 characters |
| 18 | Transaction ID | 15 characters |

See `app/app-authorization-ims-db2-mq/cpy/CCPAURQY.cpy`. There is no quoted-CSV parser, field-count validation or comprehensive date/card/amount validation in this path. Embedded commas, missing fields, over-width fields and malformed numeric input require explicit contract testing; do not infer validation from field declarations.

## Processing flow

1. `1000-INITIALIZE` retrieves the MQ trigger message with CICS `RETRIEVE`, takes its queue name and trigger data, opens the input queue shared, and attempts the first get. The saved trigger-data field is not used for decision rules.
2. `3100-READ-REQUEST-MQ` requests a five-second wait, conversion, fail-if-quiescing, and **MQGMO-NO-SYNCPOINT**. Successful gets save the incoming **CorrelId** and **ReplyToQ**. There is no fallback from a missing CorrelId to MsgId, and ReplyToQMgr is not copied into the reply destination.
3. `5000-PROCESS-AUTH` schedules `PSBPAUTB`, sets the initial decision to approve, and performs the lookups below.
4. `6000-MAKE-DECISION` builds the reply; `7100-SEND-RESPONSE` sends it.
5. If the card cross-reference was found, `8000-WRITE-AUTH-TO-DB` updates/inserts the account summary and inserts the detail.
6. The main loop increments its counter and issues CICS `SYNCPOINT`, then clears the PSB-scheduled flag. It stops on no message or when the counter is **greater than 500**, so the source permits **501** processed requests per invocation, not an exact cap of 500.

### VSAM and IMS lookups

| Lookup | Key and result |
| --- | --- |
| `CCXREF` | 16-character incoming card number → account/customer IDs |
| `ACCTDAT` | 11-digit display account ID → credit limit, current balance, cash limit |
| `CUSTDAT` | 9-digit display customer ID → customer record and found/not-found status |
| IMS `PAUTSUM0` | Packed account ID, field `ACCNTID` → existing pending-authorization summary |

The reads use CICS `RESP`/`RESP2`; VSAM `NOTFND` generates warning records, other unexpected responses are critical. The processor declares `CARDDAT` and `CARDAIX` names but does **not** read them. `5600-READ-PROFILE-DATA` is a `CONTINUE` stub.

### Implemented rules versus declared reasons

With an IMS summary, available credit is `PA-CREDIT-LIMIT - PA-CREDIT-BALANCE`. Otherwise, with a found account, it is `ACCT-CREDIT-LIMIT - ACCT-CURR-BAL`. Amounts **greater than** availability are declined. Without either a summary or found account, the decision is declined.

Approval produces response `00`, reason `0000`, and the requested amount. Decline produces response `05`, zero approved amount, and a reason chosen in this order:

| Reason | Branch |
| --- | --- |
| `3100` | Missing cross-reference/account/customer flags |
| `4100` | Insufficient credit |
| `4200`, `4300` | Card inactive, account closed flags |
| `5100`, `5200` | Card fraud, merchant fraud flags |
| `9000` | Other decline |

The latter inactive/closed/fraud flags have no rule-setting logic here. A missing customer flag participates in **reason selection after a decline**, not an independent decline check. Likewise, do not describe all VSAM lookup failures as unconditional rejection: an existing IMS summary selects its own credit branch. Card expiry, daily limits, lost-card and fraud screening are not implemented by the profile stub.

### Reply contract and delivery boundary

The response is comma-delimited: card number, transaction ID, authorization ID, response code, reason, approved amount, **including a trailing comma**. Authorization ID is copied from the incoming authorization time, not generated uniquely. Amount formatting uses an edited signed decimal field, so consumers must allow padding.

The MQ reply preserves the incoming CorrelId, has type `MQMT-REPLY`, string format, **nonpersistent** delivery, and expiry `50` MQ tenths-of-a-second units (five seconds). `MQPUT1` uses **MQPMO-NO-SYNCPOINT**. It targets the saved ReplyToQ rather than a fixed README reply-queue constant.

**There is no exactly-once or atomic request/reply/storage guarantee.** The input get is outside the CICS unit of work, the reply is outside it, and the reply occurs before IMS writes. Failure can leave a consumed request without a stored detail, or a reply without successful persistence. Critical errors log and return; the processor does not explicitly request rollback in that error routine.

## IMS accounting and modification cautions

`8400-UPDATE-SUMMARY` initializes a new root with zero numeric fields, copies account credit/cash limits, increments approval or decline totals, adds approved amounts to credit balance and resets cash balance to zero on approval. It does not initialize a new root's credit balance from the VSAM current balance. The declined-total addition references `PA-TRANSACTION-AMT` in the detail buffer **before** `8500-INSERT-AUTH` copies this request's amount there; do not assume correct current-request decline accounting.

Details contain the original request and decision plus a reverse processing-time key: `99999 - YYDDD` and `999999999 - HHMMSSmmm`, stored as two packed-decimal fields totaling eight bytes. Approved records get match status `P`; declined records get `D`; fraud fields start blank. This is not deduplication by transaction ID, and same-timestamp key collisions are not handled as idempotent replay.

`WS-RESP-LENGTH` is the `STRING` pointer initialized to 1 only in working storage. It is reused as the MQ buffer length and is not reset per loop iteration. Multi-message response framing therefore needs verification rather than assuming each iteration starts at byte one.

## Integration and modification entry points

Provision the trigger association and queues on the target host: no external POS client or complete MQ resource definitions are checked in. Preserve encoding conversion and numeric representations when adding a client. Review `5000`, `6000`, `7100`, `8400`, and `8500` together when changing decisions or recovery behavior.

Errors are formatted with `app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy` and written to CICS transient-data queue `CSSL`, carrying application/program/location, subsystem, codes, message and event key. These keys may contain card/account identifiers; host logs need appropriate access controls.

## Key source files

| Path | Role |
| --- | --- |
| `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl` | All processing paragraphs described above |
| `app/app-authorization-ims-db2-mq/cpy/CCPAURQY.cpy` | Request staging fields |
| `app/app-authorization-ims-db2-mq/cpy/CCPAURLY.cpy` | Reply staging fields |
| `app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy` | Error log contract |
| `app/app-authorization-ims-db2-mq/cpy/CIPAUSMY.cpy` | Summary accounting fields |
| `app/app-authorization-ims-db2-mq/cpy/CIPAUDTY.cpy` | Detail/key/status fields |
| `app/cpy/CVACT03Y.cpy` | Core card/account/customer cross-reference |
| `app/cpy/CVACT01Y.cpy` | Core account balances and limits |

## Related links

[IMS views and fraud marking](online-and-fraud.md) · [Purge caveats](batch-utilities.md) · [Data models](../../reference/data-models.md) · [Debugging](../../how-to-contribute/debugging.md)
