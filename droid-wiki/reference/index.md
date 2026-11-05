# Reference

[CardDemo](../overview/index.md) · [Architecture](../overview/architecture.md)

Use these pages for byte contracts and host resource names, not as a portable installation script.

| Read next | What it answers |
| --- | --- |
| [Configuration](configuration.md) | How CICS file names, batch DDs and dataset names relate; which host values need adaptation. |
| [Data models](data-models.md) | Core fixed records, VSAM keys, optional Db2 tables and IMS segment/export contracts. |
| [Dependencies](dependencies.md) | Host products, externally supplied interfaces, version limitations and licensing. |
| [Historical contacts](../maintainers.md) | Path-backed contributor history without claiming formal ownership. |

## Authority and scope

The inspected default branch ends at `8ca2f60`. Copybooks define program layouts; IDCAMS/JCL defines keys and allocations; CSD defines CICS resources; DDL and IMS definitions define optional persistent contracts. Compare these sources together when changing a field. README examples, catalog snapshots and migration templates are not substitutes for those contracts.

Behavior belongs in [core features](../features/core/index.md) and [optional systems](../systems/index.md). Exact MQ framing and recovery caveats are documented in [MQ inquiries](../systems/mq-inquiries.md) and [authorization processing](../systems/authorizations/mq-processing.md), rather than duplicated here. Bring-up belongs in [deployment](../deployment.md); sensitive-data limitations belong in [security](../security.md).

No fixture records, demonstration passwords, personal contact addresses or site-specific host identifiers are reproduced. Dataset and logical resource names below describe checked-in demonstration definitions, not a live environment.

## Key source files

| Source | Authority |
| --- | --- |
| `app/cpy/CVACT01Y.cpy`, `app/cpy/CVCUS01Y.cpy`, `app/cpy/CVTRA05Y.cpy` | Representative core records. |
| `app/csd/CARDDEMO.CSD`, `app/jcl/POSTTRAN.jcl` | Online resources and batch allocations. |
| `app/app-transaction-type-db2/ddl/TRNTYPE.ddl` | Optional reference schema. |
| `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd` | Optional IMS hierarchy. |
| `LICENSE`, `NOTICE` | Repository license and attribution. |
