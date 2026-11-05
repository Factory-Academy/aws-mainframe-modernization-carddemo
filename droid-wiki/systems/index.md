# Systems

CardDemo's optional integrations are isolated in three source directories. Shared host utilities remain in the base application. This section explains those boundaries; [CardDemo features](../features/core/index.md) covers the core user journeys.

| System | Purpose | Source |
| --- | --- | --- |
| [Authorizations](authorizations/index.md) | MQ requests, IMS pending authorizations, and Db2 fraud recording | `app/app-authorization-ims-db2-mq/` |
| [Transaction types](transaction-types.md) | Db2 reference administration and extraction into VSAM inputs | `app/app-transaction-type-db2/` |
| [MQ inquiries](mq-inquiries.md) | Separate date/account request responders | `app/app-vsam-mq/` |
| [Host utilities](utilities.md) | Date and wait routines, COBOL wrappers, and assembler interfaces | `app/asm/`, `app/maclib/`, `app/cbl/COBSWAIT.cbl`, `app/cbl/CSUTLDTC.cbl` |

Installing an optional module requires more than compiling its COBOL. Review its CSD, data declarations, host-product setup, and jobs together. [Deployment](../deployment.md) and [Configuration](../reference/configuration.md) describe those prerequisites.
