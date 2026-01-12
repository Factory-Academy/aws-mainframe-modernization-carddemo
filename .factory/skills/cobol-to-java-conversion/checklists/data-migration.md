# Data Migration Checklist

Complete this checklist for migrating data from mainframe (VSAM, DB2, IMS) to modern databases.

## 1. Pre-Migration Planning

- [ ] Data volume assessed
- [ ] Migration window identified (downtime requirements)
- [ ] Rollback strategy defined
- [ ] Data validation strategy established
- [ ] Performance requirements documented

## 2. Schema Design

- [ ] Target database schema designed
- [ ] Primary keys defined
- [ ] Foreign keys defined
- [ ] Indexes designed (based on access patterns)
- [ ] Constraints defined (NOT NULL, CHECK, etc.)
- [ ] Data types mapped from COBOL to SQL
- [ ] Schema DDL scripts created
- [ ] Schema reviewed and approved

## 3. VSAM-Specific Migration

### KSDS (Key Sequenced Data Set)
- [ ] Primary key identified
- [ ] Alternate indexes mapped to SQL indexes
- [ ] Record layout converted to table structure
- [ ] REDEFINES handled (multiple tables or JSON columns)

### ESDS (Entry Sequenced Data Set)
- [ ] Surrogate key strategy defined
- [ ] Sequential order preserved (timestamp or sequence number)

### RRDS (Relative Record Data Set)
- [ ] Record number mapping strategy defined

## 4. Data Extraction

- [ ] VSAM files unloaded to flat files
- [ ] DB2 tables exported (using DSNTIAUL or similar)
- [ ] IMS segments extracted
- [ ] Data exported with proper encoding (EBCDIC → ASCII/UTF-8)
- [ ] Extract scripts tested
- [ ] Extract performance validated

## 5. Data Transformation

- [ ] EBCDIC to ASCII/UTF-8 conversion tested
- [ ] COMP-3 (packed decimal) converted to decimal/numeric
- [ ] COMP (binary) fields converted
- [ ] Date format conversions applied
- [ ] Trailing spaces handled
- [ ] Null value strategy defined (spaces → NULL)
- [ ] Data cleansing rules applied
- [ ] Transformation scripts created

## 6. Data Type Conversions

| COBOL Type | Converted To | Validated |
|------------|--------------|-----------|
| PIC 9(n) | INTEGER/BIGINT | [ ] |
| PIC X(n) | VARCHAR(n) | [ ] |
| PIC S9(n)V99 COMP-3 | DECIMAL(n,2) | [ ] |
| PIC S9(n) COMP | INTEGER | [ ] |
| Date fields | DATE/TIMESTAMP | [ ] |

## 7. Data Loading

- [ ] Bulk load scripts created
- [ ] Load order determined (parent tables before child tables)
- [ ] Load performance tested
- [ ] Error handling implemented
- [ ] Load logs reviewed
- [ ] Referential integrity maintained

## 8. Data Validation

### Row Count Validation
- [ ] Source row counts captured
- [ ] Target row counts compared
- [ ] Discrepancies investigated

### Data Integrity Validation
- [ ] Checksums compared (if applicable)
- [ ] Sample records spot-checked
- [ ] Primary key uniqueness verified
- [ ] Foreign key relationships validated
- [ ] Data ranges validated (min/max values)
- [ ] Null value handling verified

### Business Logic Validation
- [ ] Calculated fields validated
- [ ] Aggregations compared (sums, counts, averages)
- [ ] Critical business rules verified
- [ ] Edge cases tested

## 9. Performance Validation

- [ ] Query performance tested
- [ ] Index effectiveness validated
- [ ] Bulk operations tested
- [ ] Concurrent access tested
- [ ] Performance meets or exceeds COBOL baseline

## 10. Encoding & Character Set

- [ ] EBCDIC to UTF-8/ASCII conversion verified
- [ ] Special characters handled correctly
- [ ] International characters tested (if applicable)
- [ ] Leading zeros preserved (if required)

## 11. REDEFINES Handling

For COBOL REDEFINES clauses:
- [ ] Strategy selected (multiple tables, JSON, or discriminator column)
- [ ] Data correctly populated in target structure
- [ ] Query patterns tested

## 12. Historical Data

- [ ] Archival strategy defined
- [ ] Historical data migration scope defined
- [ ] Historical data loaded and validated
- [ ] Access patterns for historical data tested

## 13. Security & Compliance

- [ ] Sensitive data encrypted at rest
- [ ] Sensitive data encrypted in transit
- [ ] Access controls implemented
- [ ] Audit trail requirements met
- [ ] Compliance requirements satisfied (PCI, HIPAA, etc.)

## 14. Backup & Recovery

- [ ] Pre-migration backup created
- [ ] Post-migration backup created
- [ ] Backup tested (restore verification)
- [ ] Recovery procedures documented

## 15. Cutover Planning

- [ ] Cutover sequence documented
- [ ] Downtime window confirmed
- [ ] Communication plan created
- [ ] Rollback triggers defined
- [ ] Go/No-go criteria established

## 16. Post-Migration

- [ ] Final data validation completed
- [ ] Performance monitoring enabled
- [ ] Data quality monitoring enabled
- [ ] Migration report created
- [ ] Lessons learned documented
- [ ] Source data archived or retained per policy

## Sign-Off

- [ ] Database administrator approval
- [ ] Data owner approval
- [ ] Quality assurance sign-off
- [ ] Business stakeholder approval

---

**Migration Date:** _______________

**Completed By:** _______________

**Data Volume Migrated:** _______________

**Migration Duration:** _______________

**Issues Encountered:** _______________

**Notes:**
