# Security

CardDemo demonstrates application flows and host integrations; its checked-in authentication and resource examples are not a production security baseline. Use isolated synthetic data and review host controls before granting access (`app/cbl/COSGN00C.cbl`, `app/csd/CARDDEMO.CSD`, `app/app-transaction-type-db2/ctl/DB2CREAT.ctl`).

## Application identity is not host authorization

Sign-on reads `USRSEC` and compares an uppercased input password directly with the stored field. The security record contains an eight-character password field; this path does not implement password hashing (`app/cbl/COSGN00C.cbl`, `app/cpy/CSUSR01Y.cpy`). User maintenance edits the application file, not RACF accounts.

Role and menu routing are carried through `app/cpy/COCOM01Y.cpy`, `app/cpy/COMEN02Y.cpy` and `app/cpy/COADM02Y.cpy`. Menu visibility alone is not proof of authorization for every directly invoked transaction/program. The base CSD includes `RESSEC(NO)` and `CMDSEC(NO)` on transactions; these settings do not establish that host-wide security is disabled, but they must not be presented as least-privilege resource enforcement (`app/csd/CARDDEMO.CSD`).

`samples/jcl/RACFCMDS.jcl` illustrates a transaction-profile change and group membership. It is not a complete approved access policy. Establish host transaction, dataset, program, job-submission and operator-command permissions separately.

## Data and diagnostic handling

Treat fixtures, security datasets, financial records, reports and spool as sensitive even in a demonstration. Do not reproduce credentials, user records, card/customer details or personal-looking fixture rows in documentation, issues or screenshots (`app/data/`, `app/cpy/CSUSR01Y.cpy`, `app/cpy/CVCUS01Y.cpy`, `app/cpy/CVACT02Y.cpy`).

Diagnostic readers can display record contents (`app/cbl/CBACT02C.cbl`, `app/cbl/CBCUS01C.cbl`). Authorization error records include a 20-character event key that can contain card/account identifiers, and are written to CSSL (`app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy`, `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`). Restrict log/spool/queue access and retention; redact keys and payloads before sharing. The repository does not supply a comprehensive audit-log or retention policy.

## Integration boundaries

| Boundary | Demonstration limitation and required review |
| --- | --- |
| MQ account inquiry | The responder returns financial account fields without an application-user authorization check. Review queue authorities, triggering identity and consumer access (`app/app-vsam-mq/cbl/COACCT01.cbl`). |
| MQ authorization | CSV extraction is not comprehensive input validation. MQ get/reply use no-syncpoint, reply is nonpersistent and precedes IMS writes; do not claim atomic or exactly-once processing (`app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`). |
| Db2 setup | The transaction-type control grants permissions to PUBLIC, including DBADM. Replace demonstration grants with an explicitly reviewed policy before broader use (`app/app-transaction-type-db2/ctl/DB2CREAT.ctl`). |
| Fraud writes | CICS syncpoint/rollback expresses coordination intent, not tested two-phase-commit assurance. Validate attachments, authority and recovery outcomes (`app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl`). |
| Online report submission | CICS writes generated JCL to `JOBS`; host internal-reader permissions and the execution identity need review (`app/cbl/CORPT00C.cbl`, `app/csd/CARDDEMO.CSD`). |
| File recovery | Base CSD resources include `RECOVERY(NONE)` and no forward recovery log. Application rollback statements alone do not establish recoverable file configuration (`app/csd/CARDDEMO.CSD`). |

See [MQ inquiries](systems/mq-inquiries.md), [authorizations](systems/authorizations/index.md) and [transaction types](systems/transaction-types.md) for exact contracts and caveats.

## Transfers and destructive operations

Remote helpers assume a local FTP tunnel and do not establish a complete secure-transfer configuration. Review the actual transport/authentication setup and keep credentials out of source, terminal captures and job cards (`scripts/remote_submit.sh`, `scripts/upld_module.sh`). Text conversion is also an integrity risk for packed/EBCDIC data; use the intended binary transfer and framing checks.

Refresh/load/purge jobs can delete or replace data. Require explicit target review, isolated datasets and restorable backups before submission (`scripts/remote_refresh.sh`, `app/jcl/TRANBKP.jcl`, `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl`). The purge has known age/count/persistence discrepancies, not a verified retention-control implementation; see [purge caveats](systems/authorizations/batch-utilities.md).

## Vulnerability reporting

Follow the private AWS vulnerability-reporting route linked in `CONTRIBUTING.md`; do not open a public issue containing a suspected vulnerability or exploit details. For ordinary defects, provide a sanitized synthetic reproduction through the [contribution workflow](how-to-contribute/index.md). These observations are source-backed limitations, not a penetration-test result or compliance certification.
