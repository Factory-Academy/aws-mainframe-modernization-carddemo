# Lore

This chronology follows all **30 reachable commits**, through `8ca2f60` on **2026-01-11**, inspected on **2026-11-05**. Dates are Git author dates, not release-service timestamps. See [By the numbers](by-the-numbers.md) for counting rules and [Modernization](background/modernization.md) for the distinction between migration guidance and an implemented replacement.

## 2022-08-22 to 2022-09-23: policy skeleton and the first application

- **2022-08-22 — `6c04f7b`, “Initial commit.”** The repository begins with five files: `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `LICENSE`, `NOTICE`, and `README.md`. All five paths survive at the inspected head. This commit does not yet contain application source.
- **2022-09-01 — `8c797e2`, “Release 1.0.”** The application arrives: COBOL under `app/cbl`, BMS under `app/bms`, shared and symbolic copybooks under `app/cpy` and `app/cpy-bms`, CICS definitions under `app/csd`, batch jobs under `app/jcl`, procedures under `app/proc`, control under `app/ctl`, and data under `app/data`. The tracked tree grows from 5 to **147 files**.
- **2022-09-01 — `1e816d0`, also “Release 1.0.”** Six diagram files bring the tree to **153 files**, including `diagrams/CARDDEMO-DataModel.drawio`, `diagrams/Application-Flow-Admin.png`, and `diagrams/Application-Flow-User.png`. The two same-day release commits add different parts of the repository.
- **2022-09-06 — `43256ba`, merged by `0a48545` the same day.** Typographical corrections update `README.md`.
- **2022-09-08 — `c4a5f1e`, merged by `9c32012` the same day.** A typo fix changes `app/jcl/DUSRSECJ.jcl`. No embedded security values are reproduced here.
- **2022-09-23 — `0a5aff7`, merged by `7a0281b` the same day.** `README.md` is corrected to match the JCL inventory.

### Surviving first-generation components

At the **2026-11-05** inspection, blame attributes all **260 physical lines** of `app/cbl/COSGN00C.cbl` and all **20 lines** of `app/cpy/CVACT01Y.cpy` to the **2022-09-01** release. The former remains a CICS sign-on program; the latter remains an account record declaration. All **175 lines** of `LICENSE` trace to **2022-08-22**. These are concrete surviving examples, not a claim that every core component is unchanged.

## 2022-10-04 to 2022-12-23: installation examples and runtime packaging

- **2022-10-04 — `120f2ba`.** The 3,956-line catalog snapshot `app/catlg/LISTCAT.txt` is added, bringing the tree to **154 files**. It is inventory evidence, not executable source.
- **2022-10-12 — `41236a8`, `be3c846`, `a71da67`, `4159f80`, merged by `ff39ba5`.** The history adds compile examples and procedures, including `samples/jcl/BATCMP.jcl`, `samples/jcl/BMSCMP.jcl`, `samples/jcl/CICCMP.jcl`, `samples/proc/BUILDBAT.prc`, `samples/proc/BUILDBMS.prc`, and `samples/proc/BUILDONL.prc`. It also changes `app/cbl/COACTUPC.cbl`, adjusts spacing in `app/cpy/CVTRA06Y.cpy`, and updates `README.md`. The merged tree reaches **160 files**.
- **2022-10-13 — `70e49ce`, merged by `73c5ac5`.** A commit explicitly labeled “Fix compile issue in CBTRN01C” changes `app/cbl/CBTRN01C.cbl`. A separate same-day commit, `e08e468`, makes application naming consistent in `README.md`.
- **2022-10-22 — `c5578b0`, merged on 2022-10-26 by `c15008d`.** Despite its “rename ACCTDATA” subject, the patch **adds** `app/data/EBCDIC/AWS.M2.CARDDEMO.ACCTDATA.PS`; `app/data/EBCDIC/AWS.M2.CARDDEMO.ACCDATA.PS` still survives. The merged tree reaches **161 files**. Git evidence does not support describing this as deletion of the older spelling.
- **2022-11-27 — `2939d3d`.** `samples/m2/mf/CardDemo_runtime.zip` is added, bringing the tree to **162 files**.
- **2022-12-02 — `563072f`.** The same archive is updated; the commit subject describes runtime configuration updates. The archive's binary diff alone does not establish a source-level feature change.
- **2022-12-23 — `09878ee`.** A small update changes `README.md`.

## 2023-01-09 to 2023-02-10: a COBOL-to-COBOL example

- **2023-01-09 — `2b89f39`, “COBOL to COBOL Call (#16).”** The tree grows to **166 files** with `app/cbl/CBSTM03A.CBL`, `app/cbl/CBSTM03B.CBL`, `app/cpy/COSTM01.CPY`, and `app/jcl/CREASTMT.JCL`; `README.md` changes alongside them. This is an added COBOL calling example, not a Java rewrite.
- **2023-02-10 — `d79b09c`.** `README.md` is updated to add copybook names. No subsequent reachable commit is dated between this change and **2024-09-17**.

## 2024-09-17 to 2024-12-09: another packaged runtime

- **2024-09-17 — `2a3caef`.** `samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip` is added; the tree reaches **167 files**.
- **2024-09-25 — `ebb8dab`.** That archive changes again. Its commit subject specifies modified compile scripts, but the repository records a binary archive replacement rather than expanded script diffs.
- **2024-12-09 — `b2452db`, “Add files via upload.”** `samples/m2/mf/CardDemo_runtime.zip` changes. The generic subject and binary diff do not establish a more specific rationale or feature claim.

## 2025-08-03: optional subsystems and host examples expand together

**2025-08-03 — `fb78417`, “Add optional modules and enhanced functionality.”** This single commit touches **190 files** and increases the tracked tree from **167 to 322 files**: a net addition of **155**.

The additions include:

- Authorization handling and host definitions under `app/app-authorization-ims-db2-mq`, spanning COBOL, IMS, MQ and Db2 integration.
- Db2 transaction-type maintenance under `app/app-transaction-type-db2`.
- MQ inquiry responders under `app/app-vsam-mq`.
- Assembler routines under `app/asm`, their macros under `app/maclib`, and the batch wait wrapper `app/cbl/COBSWAIT.cbl`.
- Additional host jobs under `app/jcl`, build examples under `samples/jcl` and `samples/proc`, and the schedule `app/scheduler/CardDemo.ca7`.
- Transfer, compilation and batch helper files under `scripts`, including 45 empty markers under `scripts/markers`.
- New subsystem diagrams under `diagrams` and broad documentation updates in `README.md`.

At the **2026-11-05** inspection, all **41 lines** of `app/cbl/COBSWAIT.cbl` blame to this **2025-08-03** commit. The wrapper reads a wait interval and calls the assembler routine; this is a surviving source-backed example of the host-utility expansion.

The file-count jump includes markers, configuration, diagrams and fixtures, not just application programs. It should not be read as 155 new features or packages.

## 2026-01-11: modernization guidance, not a replacement runtime

**2026-01-11 — `8ca2f60`, “Adding the skill for COBOL migration.”** Seventeen files under `.factory/skills/cobol-to-java-conversion` bring the tracked tree to **339 files**. They comprise guidance, checklists, mapping documents, examples, analysis/data-generation scripts and three Java templates.

For example, `.factory/skills/cobol-to-java-conversion/templates/java-service-template.java` describes itself as a service template and imports application model/repository types. Its presence is not evidence that a Java application has replaced the host implementation. The COBOL, BMS, JCL and optional subsystem trees remain checked in.

As of **2026-11-05**, this is the newest reachable commit. There are no commits in the last 90-day window, **2026-08-08 through 2026-11-05 inclusive**.

## Removals, deprecations and apparent rewrites

- **2022-10-12 — `a71da67`: one evidenced path move.** Git's rename detection reports `BATCMP.jcl` moving to `samples/jcl/BATCMP.jcl` with 100% content similarity. The old root-level location is historical, not a current source path.
- **2022-10-12 — `a71da67`: apparent large account-update churn.** The ordinary diff for `app/cbl/COACTUPC.cbl` reports thousands of changed lines. Ignoring whitespace reduces it to **4 additions and 12 deletions**. A large textual diff alone is not evidence of an architectural rewrite.
- **2025-08-03 — `fb78417`: another apparent account-update rewrite.** The same file has thousands of ordinary diff changes, but **no changes with `git diff -w`**. This does not establish replacement business logic.
- **2026-11-05 inspection, history through 2026-01-11:** `git log HEAD --find-renames --diff-filter=DR --name-status` finds the compile-job move above and **no standalone file deletions**. No dated feature-deprecation announcement, removed subsystem or completed Java rewrite was established in the inspected history. This is an evidence limit, not a promise that host behavior never changed.

COBOL `REWRITE` statements found in current source describe updating records; they are not historical rewrite announcements. Similarly, a commit title saying “rename” does not override its actual added/deleted paths.

## Growth chronology

Counts use complete tracked trees at the specified commit, not summed diff line counts. Merge commits are included where they establish the mainline snapshot.

| Date | Snapshot | Tracked files | Change represented |
|---|---|---:|---|
| 2022-08-22 | `6c04f7b` | 5 | Initial policy/documentation skeleton |
| 2022-09-01 | `8c797e2` | 147 | Application source, configuration and data |
| 2022-09-01 | `1e816d0` | 153 | Initial diagrams |
| 2022-10-04 | `120f2ba` | 154 | Catalog listing |
| 2022-10-12 | `ff39ba5` | 160 | Compile examples and procedures |
| 2022-10-26 | `c15008d` | 161 | Correctly spelled account dataset path merged |
| 2022-11-27 | `2939d3d` | 162 | First runtime archive |
| 2023-01-09 | `2b89f39` | 166 | COBOL calling/statement example |
| 2024-09-17 | `2a3caef` | 167 | UniKix runtime archive |
| 2025-08-03 | `fb78417` | 322 | Optional systems and broader host/tooling examples |
| 2026-01-11 | `8ca2f60` | 339 | Repository-local migration skill |

Evidence comes from full dated `git log`/change summaries, tracked-tree snapshots, rename detection, whitespace-insensitive diffs, selected source reads and line-level blame. Embedded version dates can predate import into this repository; chronology here uses Git evidence rather than treating embedded stamps as separate repository events. No rationale, organizational decision or upstream event is inferred from gaps in this checkout.
