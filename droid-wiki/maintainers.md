# Historical contacts

[Contribution guide](how-to-contribute/index.md) · [Lore](lore.md) · [Reference](reference/index.md)

## Ownership status

No tracked `CODEOWNERS` file was found on inspected `origin/main` at `8ca2f60`. Git history identifies people who changed a path, **not formal owners, current maintainers, assigned reviewers or a support commitment**. A historical contributor may no longer be available. Use the repository's current contribution process and issue/PR discussion to establish who can review or operate the target host.

## Path-backed contacts

Only first names or Git usernames are shown. These are historical routing clues, not a contact directory; no emails or bot/service identities are included.

| Area | Historical people | Evidence on the default branch |
| --- | --- | --- |
| Original core application and host definitions | Abhijit | `8c797e2` (2022-09-01) introduces the core source, including `app/cbl/COSGN00C.cbl`, `app/cpy/CVACT01Y.cpy`, and `app/csd/CARDDEMO.CSD`. |
| Compile examples and selected core fixes | Sanjay | `a71da67` and `4159f80` (2022-10-12) touch `samples/proc/BUILDBAT.prc`, `samples/proc/BUILDONL.prc`, and `app/cpy/CVTRA06Y.cpy`. |
| COBOL statement/calling example | `vikas020487` | `2b89f39` (2023-01-09) adds `app/cbl/CBSTM03A.CBL`, `app/cbl/CBSTM03B.CBL`, `app/cpy/COSTM01.CPY`, and `app/jcl/CREASTMT.JCL`. |
| Optional systems, runtime samples and contributor tools | Arunkumar | `fb78417` (2025-08-03) adds the three optional module directories and tooling, including `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`, `app/app-transaction-type-db2/cbl/COTRTLIC.cbl`, `app/app-vsam-mq/cbl/COACCT01.cbl`, and `scripts/local_compile.sh`; earlier commits change the runtime archives. |
| COBOL-to-Java migration guidance | Shivam | `8ca2f60` (2026-01-11) adds `.factory/skills/cobol-to-java-conversion/SKILL.md`, mappings, checklists, scripts and templates, not a deployed Java application. |

This small table is selected by technical scope, not an exhaustive contributor ranking. Scoped subsystem pages list their own historical contributors. Distinct human committers and test-file ratios do not measure current staffing, review capacity or reliability.

## How attribution was checked

Read default-branch history with path-scoped `git log origin/main --format='%h %cs %an' -- <path>` and `git log origin/main --format='%cn' -- <path>`; no email fields are needed. Author dates and committers can differ, and hosting-generated committer identities are not human owners. The rows above connect actual authors and surviving source paths, without attributing every line or every feature to a single person.

Repository process is described in `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`; those files do not establish subsystem ownership. See [development workflow](how-to-contribute/development-workflow.md) for coordinated changes and [deployment](deployment.md) for host responsibilities that must be assigned explicitly.
