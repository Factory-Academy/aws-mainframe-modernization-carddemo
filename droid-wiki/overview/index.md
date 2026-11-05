# CardDemo

CardDemo is a mainframe credit-card management demonstration used to explore migration, refactoring, and service extraction. The application is COBOL, CICS, VSAM, and JCL; optional modules exercise Db2, IMS DB, and IBM MQ. It is sample software, not a production banking platform (`README.md`).

## Start here

- [Architecture](architecture.md): online tasks, shared files, batch jobs, and optional integrations.
- [Getting started](getting-started.md): required host products and installation order.
- [CardDemo features](../features/core/index.md): accounts, cards, transactions, billing, and batch processing.
- [Systems](../systems/index.md): optional integrations and host utilities.
- [Contribution guide](../how-to-contribute/index.md): change and verification workflow.
- [Data models](../reference/data-models.md): copybooks, keys, and record formats.

## What is included

Regular users navigate account and card screens, transaction inquiry and entry, reports, and bill payments through the menu in `app/cpy/COMEN02Y.cpy`. Administrators manage users and, when installed, Db2 transaction types through `app/cpy/COADM02Y.cpy`. Signon and role routing are implemented in `app/cbl/COSGN00C.cbl`.

The three optional directories are separate extensions, not prerequisites for every screen:

| Extension | Source | Documentation |
| --- | --- | --- |
| MQ authorization processing, IMS storage, and Db2 fraud records | `app/app-authorization-ims-db2-mq/` | [Authorizations](../systems/authorizations/index.md) |
| Db2 transaction-type administration and extracts | `app/app-transaction-type-db2/` | [Transaction types](../systems/transaction-types.md) |
| Date and account inquiries over MQ | `app/app-vsam-mq/` | [MQ inquiries](../systems/mq-inquiries.md) |

## Repository map

| Path | Purpose |
| --- | --- |
| `app/cbl/`, `app/bms/`, `app/cpy/`, `app/cpy-bms/` | Core programs, terminal maps, shared records, and symbolic maps |
| `app/jcl/`, `app/proc/`, `app/ctl/`, `app/scheduler/` | Batch jobs, reusable procedures, control statements, and CA7 scheduling sample |
| `app/csd/`, `app/catlg/` | CICS resource definitions and catalog reference |
| `app/asm/`, `app/maclib/` | Assembler routines and macro interfaces |
| `app/data/` | ASCII and EBCDIC sample inputs |
| `samples/` | Compilation examples and packaged modernization runtime examples |
| `scripts/` | Environment-specific source-transfer and job-submission helpers |
| `.factory/skills/cobol-to-java-conversion/` | Conversion guidance, templates, and analysis tools |
| `diagrams/` | Existing architecture and terminal-screen images |

The Java files under the conversion skill are templates, not a built or deployed replacement application. See [Modernization](../background/modernization.md). Planned capabilities in `README.md`, such as rewards and web-service connectivity, are not treated as implemented features in this wiki.
