# Contribution guide

Contributions should be focused, reproducible and based on the current main branch. `CONTRIBUTING.md` is the authoritative contribution policy; this guide explains how to apply it to host-dependent COBOL, maps, records and jobs.

## Before starting

Check existing issues and recent pull requests, and discuss significant work in an issue before implementing it. Fork the repository, work from the latest main, and avoid unrelated reformatting (`CONTRIBUTING.md`). Follow `CODE_OF_CONDUCT.md`, retain applicable notices in `LICENSE` and `NOTICE`, and confirm contribution licensing when requested.

Choose the business boundary before editing:

| Change | Starting point |
| --- | --- |
| Sign-on, roles or menus | [Access and users](../features/core/access-and-users.md); `app/cbl/COSGN00C.cbl`, `app/cpy/COMEN02Y.cpy`, `app/cpy/COADM02Y.cpy` |
| Account/customer or card maintenance | [Accounts and cards](../features/core/accounts-and-cards.md); `app/cbl/COACTUPC.cbl`, `app/cbl/COCRDUPC.cbl` |
| Transaction entry, payment or report request | [Transactions and billing](../features/core/transactions-and-billing.md); `app/cbl/COTRN02C.cbl`, `app/cbl/COBIL00C.cbl`, `app/cbl/CORPT00C.cbl` |
| Posting, interest or statements | [Batch processing](../features/core/batch-processing.md); `app/cbl/CBTRN02C.cbl`, `app/cbl/CBACT04C.cbl`, `app/cbl/CBSTM03A.CBL` |
| Optional integrations | [Systems](../systems/index.md); separate IMS/MQ/Db2 source subtrees |
| Java conversion guidance | [Modernization](../background/modernization.md); `.factory/skills/cobol-to-java-conversion/SKILL.md` |

## Evidence for review

Include the source revision, problem, minimal reproduction, relevant local changes and unusual runtime conditions. Describe expected and actual behavior using synthetic inputs, and redact identities, record contents, connection details and credentials (`CONTRIBUTING.md`; `app/cpy/CSUSR01Y.cpy`).

Explain all coupled changes: copybook consumers, BMS/symbolic maps, CSD resources, JCL DD/DCB settings and positional SORT controls. A successful COBOL compile does not establish record compatibility or correct recovery behavior; see [development workflow](development-workflow.md) and [patterns and conventions](patterns-and-conventions.md).

`CONTRIBUTING.md` asks contributors to run local tests and respond to reported CI failures. This checkout has no checked-in CI workflow, Makefile or general unit-test harness; report the checks actually performed and mark host-only cases not run rather than inventing a green test suite. [Testing](testing.md) separates static checks from proposed runtime verification.

## Submission checklist

- Keep the patch limited to the discussed change and use clear commit messages.
- Include host compile/translation/bind results when available, plus behavioral checks and any untested integration boundary.
- Document changes to layouts, queue contracts, data initialization or restart assumptions.
- Review the diff for secrets and personal-looking data before submitting.
- Answer the pull-request questions and remain available for review (`CONTRIBUTING.md`).

Report suspected vulnerabilities through the AWS vulnerability-reporting route linked in `CONTRIBUTING.md`, not a public issue. See [security](../security.md), [tooling](tooling.md) and [debugging](debugging.md) for operational constraints.
