# Host utilities

Active contributors: Arunkumar, Abhijit

See [optional systems](index.md), [core features](../features/core/index.md), and [host conventions](../how-to-contribute/patterns-and-conventions.md).

## Purpose

Small assembler routines support date reformatting and a batch wait. A separate COBOL wrapper calls Language Environment date validation. These are distinct host interfaces, not a shared portable date/time service.

Scope: seven tracked files on `origin/main`: the two members in `app/asm`, two in `app/maclib`, `app/cbl/CSUTLDTC.cbl`, `app/cbl/COBSWAIT.cbl`, and `app/cpy/CODATECN.cpy`. Two distinct human committers appear in that scoped default-branch history. No automated tests occur in this scope: test-program-to-implementation-program ratio **0:4** (two assembler plus two COBOL programs), not coverage. Caller/job examples below are outside that count. Attribution is historical rather than formal maintenance ownership.

## Interfaces at a glance

| Entry point | Caller contract | Effect |
| --- | --- | --- |
| `COBDATFT` | One by-reference 80-byte `CODATECN-REC` group. | Copies date characters between compact and hyphenated arrangements. |
| `CSUTLDTC` | Three by-reference fields: date `X(10)`, LE date picture `X(10)`, result `X(80)`. | Calls `CEEDAYS`; returns formatted severity/message feedback and sets COBOL `RETURN-CODE` to severity. |
| `COBSWAIT` | Eight characters accepted from `SYSIN`, not a JCL `PARM=` linkage field. | Converts input to a binary interval, calls `MVSWAIT`, and stops the batch run. |
| `MVSWAIT` | Address of a fullword binary interval in centiseconds. | Expands `ASMWAIT` to `STIMER WAIT,BINTVL=...`, then returns with register 15 zero. |

## Date reformatting

`app/cpy/CODATECN.cpy` and `app/maclib/COCDATFT.mac` define the same positional group:

| Byte positions, one-based | Meaning |
| --- | --- |
| 1 | Input type: `1` = `YYYYMMDD`; `2` = `YYYY-MM-DD`. |
| 2–21 | Twenty-character input date area. |
| 22 | Output type: `1` = hyphenated; `2` = compact. Input/output type numbers intentionally have different meanings. |
| 23–42 | Twenty-character output area. |
| 43–80 | Thirty-eight-character error area. |

`app/asm/COBDATFT.asm` dispatches on input type. Input type `1` inserts hyphens and rejects output type `2`; input type `2` removes separator positions and rejects output type `1`. It otherwise accepts unrecognized output-type values instead of positively validating the allowed pair.

This routine is **not a calendar validator**: it does not check numeric content, month/day ranges, or leap years. The input-type-2 separator check is commented out. Error handling copies the literal `INVALID INPUT` into the error area but still returns register 15 zero. Successful calls do not clear prior error/output bytes; callers must initialize reusable storage and check the agreed fields rather than relying on the return register. The literal move writes only the literal's length, not a freshly padded 38-byte error message.

`app/cbl/CBACT01C.cbl`, paragraph `1300-POPUL-ACCT-RECORD`, passes account reissue date with both type selectors set to `2`, then copies the output into its exported account record. That caller does not inspect the error area. Changing the assembler or copybook therefore affects batch export bytes, not only a display.

The assembler uses static save/work areas. Do not assume the module is reentrant or suitable for concurrent CICS calls merely because its interface is callable from COBOL.

## Date validation and feedback

`app/cbl/CSUTLDTC.cbl` builds LE varying strings with ten-character lengths for the supplied date and picture, calls `CEEDAYS`, and maps feedback tokens to readable messages. The computed Lilian day stays internal; the public result is diagnostic text, not a converted date.

The 80-byte result starts with four display severity digits, eleven characters of message-label space, four message-number digits, and the remaining diagnostic message. Existing callers define these offsets explicitly:

- `app/cbl/CORPT00C.cbl` checks report start/end dates.
- `app/cbl/COTRN02C.cbl` checks transaction origin/processing dates.

Both accept severity `0000` and suppress their invalid-date path for message number `2513` even when severity is nonzero. Preserve or deliberately revise that exception when replacing validation; it is not equivalent to “every nonzero severity is rejected.”

Two naming/diagnostic caveats matter when debugging:

- The condition name `FC-INVALID-DATE` actually matches the all-zero feedback token and produces `Date is valid`; do not infer behavior from the condition name.
- After the call, the program moves the entire varying-string group into the ten-character diagnostic date field. That includes its binary length prefix rather than only the text. Treat the displayed test-date portion as potentially malformed, while severity/message-number handling remains a separate contract.

## Batch waiting

`app/cbl/COBSWAIT.cbl` accepts an eight-character `SYSIN` value, moves it to `PIC 9(8) COMP`, and calls `MVSWAIT` by reference. `app/asm/MVSWAIT.asm` dereferences that argument into a fullword and invokes `app/maclib/ASMWAIT.mac`, whose sole operation is `STIMER WAIT,BINTVL=`.

The interval unit is **centiseconds**, not seconds or milliseconds. `app/jcl/WAITSTEP.jcl` supplies `00003600`, documented as a 36-second wait. Despite comments calling it a “PARM,” the actual path is `SYSIN` → `ACCEPT` → binary argument. There is no explicit numeric/range validation in the wrapper and no returned elapsed-time/status interface. The assembler also writes `X'FF'` at offset 12 in the caller save area after restoring registers; its own inline comment questions that operation. Preserve and assess calling-convention behavior during a port instead of silently copying it into a new runtime.

## Modification and verification

- Keep `app/cpy/CODATECN.cpy` and `app/maclib/COCDATFT.mac` byte-for-byte compatible in field widths/order; coordinate any layout change with `app/asm/COBDATFT.asm` and `app/cbl/CBACT01C.cbl`.
- A replacement `CSUTLDTC` must preserve date-picture semantics, the 80-byte result offsets, severity return code, and the two online callers' message handling—not just return a boolean.
- A wait replacement must preserve centisecond conversion and batch blocking semantics. It is not a CICS delay routine or an asynchronous timer.
- On a host, check both format pairs, invalid selectors/dates, reused buffers, leap-day feedback, the `2513` caller exception, malformed wait input, and observed wait duration. No runnable local harness for these host interfaces is supplied; see [testing](../how-to-contribute/testing.md).

## Key source files

| Source | Responsibility |
| --- | --- |
| `app/asm/COBDATFT.asm` | Character-copy date conversion and error path. |
| `app/maclib/COCDATFT.mac`, `app/cpy/CODATECN.cpy` | Matching assembler/COBOL parameter layouts. |
| `app/cbl/CBACT01C.cbl` | Batch account-export date-conversion caller. |
| `app/cbl/CSUTLDTC.cbl` | LE `CEEDAYS` wrapper and formatted feedback. |
| `app/cbl/CORPT00C.cbl`, `app/cbl/COTRN02C.cbl` | Online date validation and message-number exception. |
| `app/cbl/COBSWAIT.cbl` | `SYSIN`-driven binary wait caller. |
| `app/asm/MVSWAIT.asm`, `app/maclib/ASMWAIT.mac` | Host timer invocation and assembler interface. |
| `app/jcl/WAITSTEP.jcl` | Concrete batch wait invocation. |
