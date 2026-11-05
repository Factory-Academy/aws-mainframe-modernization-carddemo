# Getting started

Reading the source requires only a Git checkout. Running CardDemo requires a configured mainframe or compatible runtime with CICS, COBOL compilation, VSAM, and JCL support; there is no checked-in laptop development server or universal build command (`README.md`, `samples/jcl/`, `samples/proc/`).

## Prerequisites

Use an isolated demonstration environment with permission to allocate datasets, compile/link programs, install CICS resources, and submit JES jobs. Optional features require the matching Db2 subsystem, IMS databases and PSBs, or MQ connection and queues. Install the base application before enabling extensions (`README.md`).

Before changing anything, identify the target high-level qualifier, load library, COBOL/copybook/map libraries, CICS region, and permitted job classes. Sample JCL uses `AWS.M2.CARDDEMO` dataset names; these are examples, not values that fit every host (`app/jcl/POSTTRAN.jcl`).

## Initial installation

The detailed installation inventory is in `README.md`. The safe bring-up order is:

1. Allocate source libraries with the documented fixed-block 80-byte source format. Transfer `app/cbl/`, `app/cpy/`, `app/bms/`, `app/asm/`, `app/maclib/`, `app/jcl/`, and `app/proc/` to their corresponding libraries.
2. Transfer EBCDIC fixtures from the actual repository path, `app/data/EBCDIC/`, in binary mode. Do not use the stale `main/-/data/EBCDIC/` path printed in the README. Match dataset record formats and lengths to copybooks and allocation JCL.
3. Adapt initialization jobs for the security, account, card, customer, cross-reference, transaction, disclosure-group, type/category, and category-balance datasets. Inspect jobs such as `app/jcl/ACCTFILE.jcl` and `app/jcl/DUSRSECJ.jcl` before submission: refresh jobs can replace data.
4. Compile BMS maps and COBOL programs using host-adjusted examples from `samples/jcl/BMSCMP.jcl`, `samples/jcl/CICCMP.jcl`, and `samples/jcl/BATCMP.jcl`, together with procedures under `samples/proc/`.
5. Adapt and install CICS definitions from `app/csd/CARDDEMO.CSD`. Verify dataset names, load libraries, transaction IDs, programs, and mapsets together.
6. Open the files and start transaction `CC00`. Confirm signon and menu routing using isolated demo users; do not reuse demonstration credentials outside the sandbox.

These are host actions, not shell commands to execute on macOS. Review [Deployment](../deployment.md) before resource changes.

## First verification

Check a read-only account/card inquiry before exercising updates. Compare displayed values against host records without copying customer or account data into issue reports. Then test navigation, input validation, and role-specific menus (`app/cbl/COSGN00C.cbl`, `app/cbl/COACTVWC.cbl`, `app/cpy/COADM02Y.cpy`).

For batch, start from a reproducible fixture snapshot, close affected CICS files, run an explicitly selected job sequence, inspect return codes and rejected records, and reopen files only after checking results. The posting inputs and outputs are visible in `app/jcl/POSTTRAN.jcl`; [Testing](../how-to-contribute/testing.md) covers the verification limits.

## What not to assume

- `scripts/local_compile.sh` references an absent sizing example. It is not a complete CardDemo build.
- `scripts/remote_compile.sh` calls a `Makefile` that is not checked in and assumes a local FTP tunnel.
- `scripts/run_full_batch.sh` submits refresh and posting jobs with fixed sleeps, not verified job-completion dependencies.
- Java templates in `.factory/skills/cobol-to-java-conversion/templates/` do not form a runnable Maven or Gradle project.

See [Tooling](../how-to-contribute/tooling.md) for script-specific assumptions, [Security](../security.md) for demonstration limitations, and [Glossary](glossary.md) for host terminology.
