# Modernization

Active contributors: Shivam

## Purpose

The repository-local conversion skill describes how to analyze COBOL and design Java replacements. Its examples cover Spring controllers/services, JPA-style repositories, data mappings, and comparison checklists. It does not contain a complete Java application or prove equivalence to CardDemo (`.factory/skills/cobol-to-java-conversion/SKILL.md`).

## Directory layout

```text
.factory/skills/cobol-to-java-conversion/
  SKILL.md
  mappings/     CICS, COBOL data types, VSAM-to-SQL examples
  examples/     Online, batch, and copybook conversions
  templates/    Illustrative Java controller, service, repository
  scripts/      Lexical COBOL analysis and synthetic data generation
  checklists/   Planning, migration, and post-conversion review
  references/   AWS Transform guidance and common pitfalls
```

The tracked scope has 17 files: 12 Markdown guidance files, three Java templates, one shell script, and one Python script. One distinct human authored changes to this directory on `origin/main`; that history count is not a guarantee of current ownership. There are no executable automated tests in this scope, so the test-to-code file ratio is 0:5 when counting the templates and scripts as code.

## Key abstractions

| Material | Intended mapping | Source |
| --- | --- | --- |
| Controller template | Terminal input/output to request/response DTOs | `.factory/skills/cobol-to-java-conversion/templates/controller-template.java` |
| Service template | COBOL paragraphs and state changes to Java business methods | `.factory/skills/cobol-to-java-conversion/templates/java-service-template.java` |
| Repository template | VSAM reads, updates, and alternate lookup paths to relational queries | `.factory/skills/cobol-to-java-conversion/templates/repository-template.java` |
| Data-type mapping | PIC clauses, decimals, arrays, and redefinitions to explicit target representations | `.factory/skills/cobol-to-java-conversion/mappings/data-types.md` |
| Validation checklist | Comparison of business results, records, rounding, errors, and transaction boundaries | `.factory/skills/cobol-to-java-conversion/checklists/post-conversion-validation.md` |

## Analysis workflow

Start from an actual source program and trace its copybooks, calls, file resources, maps, and job bindings. The lexical helper can give a first-pass inventory:

```bash
bash .factory/skills/cobol-to-java-conversion/scripts/analyze-cobol.sh app/cbl/COSGN00C.cbl
```

Run this from the repository root. The short `./scripts/analyze-cobol.sh` example in the skill document is relative to the skill directory, not the repository root.

The helper in `.factory/skills/cobol-to-java-conversion/scripts/analyze-cobol.sh` uses line-oriented pattern matches. Its comment filter only excludes an asterisk in column one, whereas fixed-format COBOL commonly places the comment marker in column seven. Multi-line `PROGRAM-ID`, `SELECT`, and CICS statements can also be missed or miscounted. Treat its output as search hints, not a parsed call graph, dependency closure, or reliable complexity measure.

## Data and template caveats

- The actual account ID is 11 display digits in `app/cpy/CVACT01Y.cpy`; examples in `.factory/skills/cobol-to-java-conversion/mappings/vsam-to-sql.md` use a different, 16-character account key. Read the real copybook and allocation JCL before designing a schema.
- `.factory/skills/cobol-to-java-conversion/scripts/generate-test-data.py` generates 16-digit account IDs and independently generated transaction account references. It is not a copybook-driven, relationally consistent fixture exporter.
- That generator uses unseeded randomness, current time, and floating-point sampling before decimal conversion. Its outputs are unsuitable as deterministic financial equivalence goldens without adaptation.
- The Java templates reference model, DTO, exception, and persistence types that are not supplied as a buildable application. The controller calls methods/signatures that do not form a consistent complete API with the service example (`.factory/skills/cobol-to-java-conversion/templates/controller-template.java`, `.factory/skills/cobol-to-java-conversion/templates/java-service-template.java`).
- Suggested transaction names and endpoint capabilities in templates are illustrative. They do not establish that corresponding core CICS transactions exist. Check `app/csd/CARDDEMO.CSD` and the actual menu/program source.

Use `BigDecimal` for financial target values, but also preserve source scale, rounding, sign representation, padding, leading zeros, and transaction behavior. Replacing a CICS syncpoint with an annotation alone does not establish equivalent coordination across MQ, IMS, and Db2. The [authorization pages](../systems/authorizations/index.md) document the source boundaries to examine.

## Entry points for modification

For a conversion, choose one existing user journey or batch boundary and establish a host baseline first. Build matching synthetic inputs from the authoritative copybooks, then compare successful outputs, rejected inputs, side effects, and rollback behavior. The checklist is a list of future validation tasks, not evidence that JUnit, coverage tooling, CI, or a REST server already exists.

See [Testing](../how-to-contribute/testing.md), [Data models](../reference/data-models.md), and [Patterns and conventions](../how-to-contribute/patterns-and-conventions.md).

## Key source files

| File | Purpose |
| --- | --- |
| `.factory/skills/cobol-to-java-conversion/SKILL.md` | Conversion workflow and target patterns |
| `.factory/skills/cobol-to-java-conversion/scripts/analyze-cobol.sh` | Lexical source inventory |
| `.factory/skills/cobol-to-java-conversion/scripts/generate-test-data.py` | Illustrative JSON/CSV generation |
| `.factory/skills/cobol-to-java-conversion/mappings/data-types.md` | Target data-type guidance |
| `.factory/skills/cobol-to-java-conversion/mappings/vsam-to-sql.md` | Illustrative schema mappings |
| `.factory/skills/cobol-to-java-conversion/templates/controller-template.java` | Controller sketch |
| `.factory/skills/cobol-to-java-conversion/templates/java-service-template.java` | Business-service sketch |
| `.factory/skills/cobol-to-java-conversion/templates/repository-template.java` | Repository sketches |
| `.factory/skills/cobol-to-java-conversion/checklists/post-conversion-validation.md` | Post-conversion verification checklist |
| `app/cpy/CVACT01Y.cpy` | Authoritative account layout for comparison |
