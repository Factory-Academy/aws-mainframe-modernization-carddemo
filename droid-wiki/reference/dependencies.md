# Dependencies

[Reference](index.md) · [Getting started](../overview/getting-started.md) · [Deployment](../deployment.md)

## Dependency model

This is a host COBOL/BMS/JCL demonstration, not a package-managed application. No checked-in package manifest, lockfile or deployed Java application was found. Dependencies are host products, translator/compiler interfaces, utility programs and installation libraries; there is no resolved transitive dependency graph or automated vulnerability audit in the repository.

### Bounded inventory

At inspected `origin/main` commit `8ca2f60`:

| Count | Meaning and limit |
| --- | --- |
| 0 package manifests/lockfiles found | No package-manager dependency count or reproducible dependency lock. Does not mean zero external dependencies. |
| 3 optional module directories | Authorization/IMS/Db2/MQ, Db2 transaction types, and MQ inquiries; not independently packaged cloud apps. |
| 8 external `COPY` targets lacking a tracked `.cpy`/`.dcl` basename | `DFHAID`, `DFHBMSCA`; `CMQGMOV`, `CMQMDV`, `CMQODV`, `CMQPMOV`, `CMQTML`, `CMQV`. Host include-path resolution is still required. |
| 4 sample build procedures | Batch, CICS online, BMS and CICS/Db2 under `samples/proc`; examples, not a portable local build. |
| 2 packaged runtime ZIPs | Micro Focus and UniKix samples; not expanded or dependency-audited here. |
| 3 Java templates | Migration guidance only; imports do not establish installed framework versions. |

The lexical scan over 42 COBOL program files finds 325 `COPY` sites and 64 distinct case-insensitive targets; repeated includes are not 325 packages. `EXEC SQL INCLUDE SQLCA` and dynamic calls are separate interfaces. Counting rules and limitations are in [by the numbers](../by-the-numbers.md#dependency-and-call-complexity-proxies).

## Host products and interfaces

| Dependency | Where required | Source evidence |
| --- | --- | --- |
| z/OS-style batch/JES and dataset services | Native JCL execution, allocation, GDGs, internal reader, catalog/VSAM operations | `app/jcl/POSTTRAN.jcl`, `app/jcl/DEFGDGB.jcl`, `app/csd/CARDDEMO.CSD` |
| COBOL compiler, binder and Language Environment | Program compilation/linkage, date validation | `samples/proc/BUILDBAT.prc` uses `IGYCRCTL`/`HEWL`; `app/cbl/CSUTLDTC.cbl` calls `CEEDAYS`. |
| CICS and BMS toolchain/runtime | Online tasks, terminal maps, file access and host transactions | `samples/proc/BUILDONL.prc`, `samples/proc/BUILDBMS.prc`, `app/cbl/COSGN00C.cbl` |
| Assembler and host timer/macros | BMS generation and shared assembler routines | `samples/proc/BUILDBMS.prc` uses `ASMA90`; `app/asm/MVSWAIT.asm`, `app/maclib/ASMWAIT.mac`. |
| VSAM/IDCAMS and batch utility services | Master/reference stores, AIXes, loads/backups; sorting and copying | `app/jcl/CARDFILE.jcl`, `app/jcl/XREFFILE.jcl`, `app/jcl/TRANREPT.jcl` |
| Db2 precompiler, attachments, packages/plans and utilities | Optional reference maintenance and authorization fraud writes | `samples/proc/BLDCIDB2.prc`, `app/app-transaction-type-db2/jcl/TRANEXTR.jcl`, `app/app-authorization-ims-db2-mq/cbl/COPAUS2C.cbl` |
| IMS DL/I, DBD/PSB generation and utilities | Optional authorization hierarchy, BMP/DLI programs and unloads | `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd`, `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl` |
| IBM MQ, COBOL copybooks and CICS attachment/trigger setup | Optional authorization and inquiry responders | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`, `app/app-vsam-mq/cbl/COACCT01.cbl` |
| Host security/admin tooling | Example RACF authority and SDSF-driven region commands | `samples/jcl/RACFCMDS.jcl`, `app/jcl/CARDFILE.jcl`; not a production security policy. |
| CA7 scheduler | Optional supplied schedule only | `app/scheduler/CardDemo.ca7`; not required merely to read/compile the source. |
| FTP/TXT2PDF and utility setup | Optional transfer and statement-conversion jobs | `app/jcl/FTPJCL.JCL`, `app/jcl/TXT2PDF1.JCL`; target facilities must be installed separately. |

Product availability, entitlements, host include libraries, subsystem attachments and recovery configuration must be supplied by the target environment. The repository does not bundle CICS, MQ, IMS or Db2 into a local development container.

## Versions and portability

There is **no authoritative pinned/supported version matrix**. Sample procedure dataset names encode product-release hints, but installation library names are not lockfiles or compatibility certification. Source `Ver:` comments identify a CardDemo snapshot, not a compiler/runtime dependency version. No specific minimum product version is established here.

`samples/m2/mf/CardDemo_runtime.zip` and `samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip` are separate replatforming samples. Their existence does not prove every optional IBM-specific integration runs unchanged, nor that a named ZIP suffix pins all bundled dependencies. This documentation does not expand or audit those archives.

The repository-local migration skill's `.factory/skills/cobol-to-java-conversion/templates/controller-template.java`, `.factory/skills/cobol-to-java-conversion/templates/java-service-template.java`, and `.factory/skills/cobol-to-java-conversion/templates/repository-template.java` illustrate Java/Spring-style structures. No Maven/Gradle project or working replacement service accompanies them. See [modernization](../background/modernization.md) rather than treating template imports as an installed dependency bill of materials.

Contributor tools use shell/SSH/transfer/host-compiler facilities and a synthetic-data Python script. They do not provision a portable application runtime; see [tooling](../how-to-contribute/tooling.md). No helper script, archive installation or host batch job was executed for this reference.

## License

The repository is licensed under **Apache License 2.0**, as recorded in `LICENSE`. `NOTICE` attributes copyright to Amazon.com, Inc. or its affiliates. Preserve the applicable license/notice and modification notices when redistributing source or derived work.

Apache licensing of this repository does not grant licenses to proprietary host products or establish license provenance for everything in a runtime archive. Check the target products and any redistributed bundled components separately. The license provides the work “AS IS”; this demonstration is not a support, security or banking-correctness guarantee.
