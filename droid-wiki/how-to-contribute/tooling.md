# Tooling

The repository provides sample host build jobs and small transfer/submission helpers, not a self-contained developer toolchain. Read and adapt them before use: several alter source, Git tags or host datasets (`scripts/upld_module.sh`, `scripts/git-addSrcVersionInfo.sh`, `scripts/remote_refresh.sh`).

## Host build examples

| Entry | Companion or behavior |
| --- | --- |
| `samples/jcl/BATCMP.jcl` | Calls `samples/proc/BUILDBAT.prc` for COBOL compilation, listing and link-edit. |
| `samples/jcl/CICCMP.jcl` | Calls `samples/proc/BUILDONL.prc`, then an SDSF NEWCOPY example. |
| `samples/jcl/BMSCMP.jcl` | Calls `samples/proc/BUILDBMS.prc` to assemble/link the map and generate its symbolic copybook; also contains NEWCOPY. |
| `samples/jcl/CICDBCMP.jcl` | Calls `samples/proc/BLDCIDB2.prc` for SQL precompile, CICS translation, compile/link and package/plan binds. |
| `samples/jcl/IMSMQCMP.jcl` | Inline CICS/DLI translation and COBOL/link example with IMS/MQ libraries and MQ stub. |
| `samples/jcl/LISTCAT.jcl`, `samples/jcl/REPRTEST.jcl`, `samples/jcl/SORTTEST.jcl` | Catalog, copy and SORT examples; not assertion-based tests. |

Symbols, compiler/product libraries, procedure-library naming and region commands need host-specific review. Db2 plan names must agree with installed DB2ENTRY resources. No single laptop compiler invocation replaces the required CICS, IMS, MQ and Db2 processing.

## Script inventory and hazards

| Script | Actual behavior and limitation |
| --- | --- |
| `scripts/local_compile.sh` | Invokes `cobc` with an absent `sample/cobol/SIZING.cbl`; not a CardDemo build. |
| `scripts/remote_compile.sh` | Runs `make -f Makefile` although none is checked in, substitutes a member in `scripts/compile_batch.jcl.template`, and submits via FTP/JES. The relative template path assumes a particular working directory; failed make is not explicitly gated before later work. |
| `scripts/remote_submit.sh` | Accepts only the literal lowercase `.jcl` extension and submits the supplied file through FTP/JES; uppercase `.JCL` sources also exist. It does not wait for job completion or inspect results. |
| `scripts/upld_module.sh` | Pads and **overwrites the input source locally**, then uploads a member to a fixed sample dataset prefix. Its lowercase-extension member-extraction regex is not a general filename parser; paths are not robustly quoted. |
| `scripts/pad.awk` | Replaces carriage returns with spaces and uses `%-80s`: this pads short lines but **does not truncate lines over 80 characters**, despite its comment. |
| `scripts/remote_refresh.sh` | Submits close, initialization/refresh and reopen jobs. It can replace account/card/customer/transaction/security and reference data. |
| `scripts/run_posting.sh` | Includes account/category refresh and transaction backup/redefinition before posting; not merely a nondestructive posting wrapper. |
| `scripts/run_interest_calc.sh` | Submits interest, transaction backup/redefinition, combine and index jobs. |
| `scripts/run_full_batch.sh` | Mixes refresh, posting, interest, backup/combine, index and reopen submissions. It is not an installed scheduler or a completion-aware workflow. |
| `scripts/git-addSrcVersionInfo.sh` | Rewrites a source version comment, strips CRs and **deletes local Git tags other than its configured version**, then attempts to create that version tag. Do not run as harmless formatting. |

The remote scripts assume `tnftp` and a local FTP tunnel on port 2121. Their process-text check is not a connectivity/authentication check. Relative paths differ: batch wrappers reference `jcl/` beneath the working directory, the uploader references `scripts/pad.awk`, and remote compilation references a template in the current directory. There is no one working directory that makes every assumption universally correct.

The upload helper does not explicitly select binary mode; it is not a safe generic transfer tool for EBCDIC fixtures or packed records. Check [testing](testing.md) for encoding/framing validation and [security](../security.md) for transfer controls.

## Submission is not completion

FTP `SITE FILETYPE=JES` plus `put` submits work; it does not prove the job finished. The batch wrappers use fixed five/ten-second sleeps and do not capture dependent job completion or inspect step results (`scripts/run_full_batch.sh`, `scripts/run_posting.sh`, `scripts/run_interest_calc.sh`).

Use host-approved submission and job-result inspection, verify each prerequisite's completion, and stop on unexpected outcomes before starting dependent work or reopening files. This is recommended operator discipline, not functionality already implemented in the helpers. `app/scheduler/CardDemo.ca7` is an example schedule listing; its repeated jobs and schedule IDs must not be flattened into the shell scripts' order.

`scripts/markers/` contains transfer/build marker artifacts, not a test suite or authoritative proof that a host member is current. Compare the actual transferred/installed member and listing instead.

See [development workflow](development-workflow.md), [deployment](../deployment.md) and [host utilities](../systems/utilities.md). `app/jcl/WAITSTEP.jcl` demonstrates a timed wait, not a dependency or job-success check.
