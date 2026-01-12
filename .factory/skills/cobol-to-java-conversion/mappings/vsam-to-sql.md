# VSAM to SQL Database Migration

Guide for converting VSAM file structures to relational database tables.

## VSAM File Types

| VSAM Type | Description | SQL Equivalent |
|-----------|-------------|----------------|
| KSDS (Key Sequenced Data Set) | Indexed file with primary key | Table with PRIMARY KEY |
| ESDS (Entry Sequenced Data Set) | Sequential file, no key | Table with auto-increment ID |
| RRDS (Relative Record Data Set) | Direct access by record number | Table with record number as key |
| AIX (Alternate Index) | Secondary index on KSDS | SQL INDEX on table |

## KSDS → SQL Table with Primary Key

### COBOL/JCL Definition

```jcl
//DEFINE  EXEC PGM=IDCAMS
//SYSIN   DD *
  DEFINE CLUSTER                         -
    (NAME(AWS.M2.CARDDEMO.ACCTDATA)     -
     INDEXED                             -
     KEYS(16 0)                          -
     RECORDSIZE(300 300)                 -
     VOLUMES(SMS001)                     -
     CYLINDERS(10 5))                    -
  DATA                                   -
    (NAME(AWS.M2.CARDDEMO.ACCTDATA.DATA))-
  INDEX                                  -
    (NAME(AWS.M2.CARDDEMO.ACCTDATA.INDEX))
/*
```

**COBOL Record Layout:**
```cobol
01  ACCOUNT-RECORD.
    05  ACCT-ID             PIC X(16).     *> Primary Key
    05  ACCT-ACTIVE-STATUS  PIC X(01).
    05  ACCT-CURR-BAL       PIC S9(13)V99 COMP-3.
    05  ACCT-CREDIT-LIMIT   PIC S9(13)V99 COMP-3.
    05  ACCT-CASH-CREDIT-LIMIT PIC S9(13)V99 COMP-3.
    05  ACCT-OPEN-DATE      PIC X(10).
    05  ACCT-EXPIRATION-DATE PIC X(10).
    05  ACCT-REISSUE-DATE   PIC X(10).
    05  ACCT-CURR-CYC-CREDIT PIC S9(13)V99 COMP-3.
    05  ACCT-CURR-CYC-DEBIT PIC S9(13)V99 COMP-3.
    05  ACCT-GROUP-ID       PIC X(10).
```

### SQL Table Definition

```sql
CREATE TABLE account (
    acct_id                 VARCHAR(16) PRIMARY KEY,
    acct_active_status      CHAR(1) NOT NULL,
    acct_curr_bal           DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    acct_credit_limit       DECIMAL(15,2) NOT NULL,
    acct_cash_credit_limit  DECIMAL(15,2),
    acct_open_date          DATE NOT NULL,
    acct_expiration_date    DATE,
    acct_reissue_date       DATE,
    acct_curr_cyc_credit    DECIMAL(15,2) DEFAULT 0.00,
    acct_curr_cyc_debit     DECIMAL(15,2) DEFAULT 0.00,
    acct_group_id           VARCHAR(10),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_account_status ON account(acct_active_status);
CREATE INDEX idx_account_group ON account(acct_group_id);
```

## Alternate Index (AIX) → SQL Index

### COBOL/JCL AIX Definition

```jcl
//DEFINE  EXEC PGM=IDCAMS
//SYSIN   DD *
  DEFINE AIX                             -
    (NAME(AWS.M2.CARDDEMO.CARDXREF.AIX) -
     RELATE(AWS.M2.CARDDEMO.CARDDATA)   -
     KEYS(16 0)                          -
     RECORDSIZE(50 50)                   -
     UPGRADE                             -
     UNIQUEKEY)                          -
  DATA                                   -
    (NAME(AWS.M2.CARDDEMO.CARDXREF.AIX.DATA))
/*
```

**This AIX allows looking up by CARD-NUM instead of just CUST-ID.**

### SQL Equivalent

```sql
-- Main table (KSDS)
CREATE TABLE card_data (
    cust_id         VARCHAR(16) PRIMARY KEY,
    card_num        VARCHAR(16) UNIQUE NOT NULL,
    card_status     CHAR(1),
    card_balance    DECIMAL(15,2),
    -- ... other fields
);

-- Index equivalent to AIX
CREATE UNIQUE INDEX idx_card_num ON card_data(card_num);

-- Or use it as alternate key
ALTER TABLE card_data ADD CONSTRAINT uk_card_num UNIQUE (card_num);
```

## ESDS → SQL Table with Auto-Increment

**COBOL Sequential File:**
```cobol
01  TRANSACTION-RECORD.
    05  TRAN-DATA           PIC X(350).
```

### SQL Table

```sql
CREATE TABLE transaction_log (
    log_id          BIGSERIAL PRIMARY KEY,  -- Auto-increment surrogate key
    tran_data       VARCHAR(350),
    load_timestamp  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sequence_num    BIGINT  -- Preserve original sequence if needed
);
```

## RRDS → SQL Table

**COBOL Relative Record:**
```cobol
01  RECORD-NUMBER           PIC 9(9) COMP.
01  DATA-RECORD             PIC X(200).
```

### SQL Table

```sql
CREATE TABLE relative_data (
    record_number   INTEGER PRIMARY KEY,
    data_record     VARCHAR(200),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Cross-Reference Files

**COBOL Cross-Reference (KSDS with multiple relationships):**
```cobol
01  XREF-RECORD.
    05  XREF-CARD-NUM       PIC X(16).     *> Primary Key
    05  XREF-CUST-ID        PIC 9(09) COMP.
    05  XREF-ACCT-ID        PIC 9(11) COMP.
```

### SQL with Foreign Keys

```sql
CREATE TABLE card_xref (
    card_num        VARCHAR(16) PRIMARY KEY,
    cust_id         BIGINT NOT NULL,
    acct_id         BIGINT NOT NULL,
    
    FOREIGN KEY (cust_id) REFERENCES customer(cust_id),
    FOREIGN KEY (acct_id) REFERENCES account(acct_id)
);

CREATE INDEX idx_xref_cust ON card_xref(cust_id);
CREATE INDEX idx_xref_acct ON card_xref(acct_id);
```

## REDEFINES → Multiple Tables or JSON

### Option 1: Separate Tables (Normalized)

**COBOL with REDEFINES:**
```cobol
01  TRANSACTION-RECORD.
    05  TRAN-ID         PIC X(16).
    05  TRAN-TYPE       PIC X(02).
    05  TRAN-DATA       PIC X(200).
    05  DEBIT-TRAN REDEFINES TRAN-DATA.
        10  DEBIT-AMOUNT    PIC S9(13)V99 COMP-3.
        10  DEBIT-ACCOUNT   PIC X(16).
        10  FILLER          PIC X(171).
    05  CREDIT-TRAN REDEFINES TRAN-DATA.
        10  CREDIT-AMOUNT   PIC S9(13)V99 COMP-3.
        10  CREDIT-ACCOUNT  PIC X(16).
        10  FILLER          PIC X(171).
```

**SQL Separate Tables:**
```sql
-- Base transaction table
CREATE TABLE transaction (
    tran_id         VARCHAR(16) PRIMARY KEY,
    tran_type       VARCHAR(2) NOT NULL,
    tran_timestamp  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Debit-specific fields
CREATE TABLE debit_transaction (
    tran_id         VARCHAR(16) PRIMARY KEY,
    debit_amount    DECIMAL(15,2) NOT NULL,
    debit_account   VARCHAR(16) NOT NULL,
    
    FOREIGN KEY (tran_id) REFERENCES transaction(tran_id)
);

-- Credit-specific fields
CREATE TABLE credit_transaction (
    tran_id         VARCHAR(16) PRIMARY KEY,
    credit_amount   DECIMAL(15,2) NOT NULL,
    credit_account  VARCHAR(16) NOT NULL,
    
    FOREIGN KEY (tran_id) REFERENCES transaction(tran_id)
);
```

### Option 2: Single Table with Discriminator

```sql
CREATE TABLE transaction (
    tran_id         VARCHAR(16) PRIMARY KEY,
    tran_type       VARCHAR(2) NOT NULL,  -- 'DB' or 'CR'
    
    -- Debit fields (NULL for credit transactions)
    debit_amount    DECIMAL(15,2),
    debit_account   VARCHAR(16),
    
    -- Credit fields (NULL for debit transactions)
    credit_amount   DECIMAL(15,2),
    credit_account  VARCHAR(16),
    
    tran_timestamp  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CHECK (
        (tran_type = 'DB' AND debit_amount IS NOT NULL) OR
        (tran_type = 'CR' AND credit_amount IS NOT NULL)
    )
);
```

### Option 3: JSON Column (PostgreSQL, MySQL 8+)

```sql
CREATE TABLE transaction (
    tran_id         VARCHAR(16) PRIMARY KEY,
    tran_type       VARCHAR(2) NOT NULL,
    tran_data       JSONB,  -- Store varying structure as JSON
    tran_timestamp  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index on JSON fields (PostgreSQL)
CREATE INDEX idx_tran_debit_amount ON transaction 
    USING GIN ((tran_data->'debit_amount'));
```

## OCCURS (Arrays) → Child Table

**COBOL with OCCURS:**
```cobol
01  CUSTOMER-RECORD.
    05  CUST-ID             PIC X(09).
    05  CUST-NAME           PIC X(50).
    05  PHONE-NUMBERS.
        10  PHONE-NUMBER    PIC X(20) OCCURS 5 TIMES.
```

**SQL with Child Table:**
```sql
CREATE TABLE customer (
    cust_id         VARCHAR(9) PRIMARY KEY,
    cust_name       VARCHAR(50) NOT NULL
);

CREATE TABLE customer_phone (
    phone_id        SERIAL PRIMARY KEY,
    cust_id         VARCHAR(9) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    phone_sequence  INTEGER NOT NULL,  -- 1 to 5, preserves order
    
    FOREIGN KEY (cust_id) REFERENCES customer(cust_id),
    UNIQUE (cust_id, phone_sequence)
);
```

## Data Migration Process

### 1. Extract VSAM Data

```bash
# Unload VSAM to flat file
//UNLOAD EXEC PGM=IDCAMS
//SYSIN   DD *
  REPRO INFILE(ACCTDAT) OUTFILE(ACCTFLAT)
/*
//ACCTDAT  DD DSN=AWS.M2.CARDDEMO.ACCTDATA,DISP=SHR
//ACCTFLAT DD DSN=AWS.M2.CARDDEMO.ACCTDATA.FLAT,
//            DISP=(NEW,CATLG),
//            SPACE=(CYL,(10,5)),
//            DCB=(RECFM=FB,LRECL=300)
```

### 2. Convert EBCDIC to ASCII/UTF-8

```bash
# Using iconv
iconv -f IBM037 -t UTF-8 < input.ebcdic > output.utf8

# Or using dd
dd if=input.ebcdic of=output.ascii conv=ascii
```

### 3. Parse and Transform

```python
# Python script to parse fixed-length records
import struct
from decimal import Decimal

def parse_comp3(data):
    """Parse COMP-3 (packed decimal) field"""
    # COMP-3 encoding: 2 digits per byte, sign in low nibble of last byte
    pass

def parse_account_record(line):
    """Parse 300-byte account record"""
    record = {
        'acct_id': line[0:16].strip(),
        'acct_active_status': line[16:17],
        'acct_curr_bal': parse_comp3(line[17:25]),  # 8 bytes COMP-3
        # ... parse other fields
    }
    return record
```

### 4. Load into SQL Database

```sql
-- PostgreSQL COPY command (fast bulk load)
COPY account FROM '/path/to/data.csv' 
    WITH (FORMAT csv, HEADER true, DELIMITER ',');

-- MySQL LOAD DATA
LOAD DATA INFILE '/path/to/data.csv'
INTO TABLE account
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;
```

### 5. Verify Data

```sql
-- Compare row counts
SELECT COUNT(*) FROM account;
-- Expected: matches VSAM record count

-- Validate key uniqueness
SELECT acct_id, COUNT(*) 
FROM account 
GROUP BY acct_id 
HAVING COUNT(*) > 1;
-- Expected: no rows (all unique)

-- Check for NULL primary keys
SELECT COUNT(*) FROM account WHERE acct_id IS NULL;
-- Expected: 0

-- Validate decimal precision
SELECT SUM(acct_curr_bal) FROM account;
-- Compare with COBOL total

-- Sample data comparison
SELECT * FROM account WHERE acct_id = '1234567890123456';
-- Manually verify against COBOL output
```

## Performance Considerations

### Indexing Strategy

```sql
-- Primary key (always indexed)
PRIMARY KEY (acct_id)

-- Frequently queried fields
CREATE INDEX idx_account_status ON account(acct_active_status);
CREATE INDEX idx_account_group ON account(acct_group_id);

-- Composite indexes for common queries
CREATE INDEX idx_status_balance ON account(acct_active_status, acct_curr_bal);

-- Covering index (includes all queried columns)
CREATE INDEX idx_account_summary 
    ON account(acct_id, acct_active_status, acct_curr_bal, acct_credit_limit);
```

### Partitioning (for large tables)

```sql
-- Range partitioning by date
CREATE TABLE transaction (
    tran_id         VARCHAR(16) NOT NULL,
    tran_date       DATE NOT NULL,
    tran_amount     DECIMAL(15,2),
    -- ...
) PARTITION BY RANGE (tran_date);

CREATE TABLE transaction_2024_q1 PARTITION OF transaction
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
    
CREATE TABLE transaction_2024_q2 PARTITION OF transaction
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
```

## Schema Evolution

Plan for future changes:

```sql
-- Add audit columns
ALTER TABLE account ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE account ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE account ADD COLUMN created_by VARCHAR(50);
ALTER TABLE account ADD COLUMN updated_by VARCHAR(50);

-- Add soft delete
ALTER TABLE account ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE account ADD COLUMN deleted_at TIMESTAMP;
```

## Common Pitfalls

### ❌ DON'T: Lose leading zeros
```sql
-- WRONG: Storing numeric account ID loses leading zeros
acct_id BIGINT  -- '0001234567' becomes 1234567
```

### ✅ DO: Use VARCHAR for alphanumeric keys
```sql
-- CORRECT: Preserve leading zeros
acct_id VARCHAR(16)  -- '0001234567' stays '0001234567'
```

### ❌ DON'T: Use FLOAT for money
```sql
-- WRONG: Floating point for currency
balance FLOAT  -- Precision loss
```

### ✅ DO: Use DECIMAL for money
```sql
-- CORRECT: Fixed-point decimal
balance DECIMAL(15,2)  -- Exact precision
```

### ❌ DON'T: Forget to handle spaces
```sql
-- WRONG: COBOL spaces not trimmed
INSERT INTO customer VALUES ('JOHN      ', ...);  -- Trailing spaces
```

### ✅ DO: Trim spaces during migration
```python
name = cobol_record.name.strip()  # Remove trailing spaces
```

## Summary Checklist

- [ ] VSAM file types identified (KSDS, ESDS, RRDS)
- [ ] Primary keys mapped
- [ ] Alternate indexes converted to SQL indexes
- [ ] REDEFINES strategy selected (separate tables, discriminator, or JSON)
- [ ] OCCURS converted to child tables
- [ ] COMP-3 fields mapped to DECIMAL
- [ ] Date fields converted to proper DATE/TIMESTAMP types
- [ ] Foreign key relationships established
- [ ] Indexes created for performance
- [ ] Data extracted and converted (EBCDIC → UTF-8)
- [ ] Data loaded into SQL database
- [ ] Row counts verified
- [ ] Sample data spot-checked
- [ ] Performance tested
