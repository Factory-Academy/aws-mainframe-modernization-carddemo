# Copybook to POJO Conversion Example

Example of converting COBOL copybooks to Java POJOs.

## Simple Copybook

### COBOL Copybook

```cobol
      ******************************************************************
      * Copybook: CVCUS01Y
      * Purpose:  Customer Record Structure
      ******************************************************************
       01  CUSTOMER-RECORD.
           05  CUST-ID                 PIC 9(09) COMP.
           05  CUST-FIRST-NAME         PIC X(25).
           05  CUST-LAST-NAME          PIC X(25).
           05  CUST-ADDR-LINE-1        PIC X(50).
           05  CUST-ADDR-LINE-2        PIC X(50).
           05  CUST-CITY               PIC X(50).
           05  CUST-STATE-CD           PIC X(02).
           05  CUST-ZIP-CD             PIC X(10).
           05  CUST-PHONE-NUM-1        PIC X(15).
           05  CUST-PHONE-NUM-2        PIC X(15).
           05  CUST-BIRTH-DATE         PIC X(10).
           05  CUST-FICO-CREDIT-SCORE  PIC 9(03) COMP.
```

### Java POJO

```java
package com.carddemo.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "customer")
public class Customer {
    
    @Id
    @Column(name = "cust_id")
    private Long custId;  // PIC 9(09) COMP → Long
    
    @Column(name = "first_name", length = 25, nullable = false)
    @NotBlank(message = "First name is required")
    private String firstName;  // PIC X(25) → String
    
    @Column(name = "last_name", length = 25, nullable = false)
    @NotBlank(message = "Last name is required")
    private String lastName;  // PIC X(25) → String
    
    @Embedded
    private Address address;  // Group item → Embeddable
    
    @Column(name = "phone_1", length = 15)
    @Pattern(regexp = "\\d{3}-\\d{3}-\\d{4}")
    private String phoneNumber1;  // PIC X(15) → String
    
    @Column(name = "phone_2", length = 15)
    @Pattern(regexp = "\\d{3}-\\d{3}-\\d{4}")
    private String phoneNumber2;  // PIC X(15) → String
    
    @Column(name = "birth_date")
    private LocalDate birthDate;  // PIC X(10) → LocalDate
    
    @Column(name = "fico_score")
    @Min(300)
    @Max(850)
    private Integer ficoScore;  // PIC 9(03) COMP → Integer
    
    // Getters, setters, constructors
    
    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getAge() {
        return LocalDate.now().getYear() - birthDate.getYear();
    }
}

@Embeddable
class Address {
    @Column(name = "addr_line_1", length = 50)
    private String addressLine1;
    
    @Column(name = "addr_line_2", length = 50)
    private String addressLine2;
    
    @Column(name = "city", length = 50)
    private String city;
    
    @Column(name = "state", length = 2)
    private String state;
    
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    
    // Getters, setters
}
```

## Complex Copybook with COMP-3 and Arrays

### COBOL Copybook

```cobol
      ******************************************************************
      * Copybook: CVACT01Y
      * Purpose:  Account Record Structure
      ******************************************************************
       01  ACCOUNT-RECORD.
           05  ACCT-ID                    PIC 9(11) COMP.
           05  ACCT-ACTIVE-STATUS         PIC X(01).
           05  ACCT-CURR-BAL              PIC S9(13)V99 COMP-3.
           05  ACCT-CREDIT-LIMIT          PIC S9(13)V99 COMP-3.
           05  ACCT-CASH-CREDIT-LIMIT     PIC S9(13)V99 COMP-3.
           05  ACCT-OPEN-DATE             PIC X(10).
           05  ACCT-EXPIRATION-DATE       PIC X(10).
           05  ACCT-REISSUE-DATE          PIC X(10).
           05  ACCT-CURR-CYC-CREDIT       PIC S9(13)V99 COMP-3.
           05  ACCT-CURR-CYC-DEBIT        PIC S9(13)V99 COMP-3.
           05  ACCT-ADDR-ZIP              PIC X(10).
           05  ACCT-GROUP-ID              PIC X(10).
           05  ACCT-MONTHLY-BALANCES.
               10  MONTHLY-BAL            PIC S9(13)V99 COMP-3
                                          OCCURS 12 TIMES.
```

### Java POJO

```java
package com.carddemo.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
public class Account {
    
    @Id
    @Column(name = "acct_id")
    private Long accountId;  // PIC 9(11) COMP → Long
    
    @Column(name = "active_status", length = 1)
    private String activeStatus;  // PIC X(01) → String
    
    @Column(name = "current_balance", precision = 15, scale = 2)
    private BigDecimal currentBalance;  // PIC S9(13)V99 COMP-3 → BigDecimal
    
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;  // PIC S9(13)V99 COMP-3 → BigDecimal
    
    @Column(name = "cash_credit_limit", precision = 15, scale = 2)
    private BigDecimal cashCreditLimit;  // PIC S9(13)V99 COMP-3 → BigDecimal
    
    @Column(name = "open_date")
    private LocalDate openDate;  // PIC X(10) → LocalDate
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;  // PIC X(10) → LocalDate
    
    @Column(name = "reissue_date")
    private LocalDate reissueDate;  // PIC X(10) → LocalDate
    
    @Column(name = "curr_cyc_credit", precision = 15, scale = 2)
    private BigDecimal currentCycleCredit;  // PIC S9(13)V99 COMP-3 → BigDecimal
    
    @Column(name = "curr_cyc_debit", precision = 15, scale = 2)
    private BigDecimal currentCycleDebit;  // PIC S9(13)V99 COMP-3 → BigDecimal
    
    @Column(name = "addr_zip", length = 10)
    private String addressZip;  // PIC X(10) → String
    
    @Column(name = "group_id", length = 10)
    private String groupId;  // PIC X(10) → String
    
    // OCCURS → Child table relationship
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("month ASC")
    private List<MonthlyBalance> monthlyBalances = new ArrayList<>();
    
    // Helper methods
    public boolean isActive() {
        return "A".equals(activeStatus);
    }
    
    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(currentBalance);
    }
    
    public void addMonthlyBalance(int month, BigDecimal balance) {
        MonthlyBalance mb = new MonthlyBalance();
        mb.setAccount(this);
        mb.setMonth(month);
        mb.setBalance(balance);
        monthlyBalances.add(mb);
    }
    
    // Getters, setters, constructors
}

@Entity
@Table(name = "monthly_balance")
class MonthlyBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acct_id", nullable = false)
    private Account account;
    
    @Column(name = "month", nullable = false)
    private Integer month;  // 1-12
    
    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance;  // COMP-3 → BigDecimal
    
    // Getters, setters
}
```

## Copybook with REDEFINES

### COBOL Copybook

```cobol
      ******************************************************************
      * Copybook: CVTRA05Y
      * Purpose:  Transaction Record with REDEFINES
      ******************************************************************
       01  TRANSACTION-RECORD.
           05  TRAN-ID                 PIC X(16).
           05  TRAN-TYPE-CD            PIC X(02).
           88  TRAN-TYPE-DEBIT         VALUE 'DB'.
           88  TRAN-TYPE-CREDIT        VALUE 'CR'.
           88  TRAN-TYPE-PAYMENT       VALUE 'PY'.
           05  TRAN-CAT-CD             PIC 9(04) COMP.
           05  TRAN-SOURCE             PIC X(10).
           05  TRAN-DESC               PIC X(100).
           05  TRAN-AMT                PIC S9(13)V99 COMP-3.
           05  TRAN-CARD-NUM           PIC X(16).
           05  TRAN-MERCHANT-ID        PIC 9(15) COMP.
           05  TRAN-MERCHANT-NAME      PIC X(50).
           05  TRAN-MERCHANT-CITY      PIC X(50).
           05  TRAN-MERCHANT-ZIP       PIC X(10).
           05  TRAN-ORIG-TS            PIC X(26).
           05  TRAN-PROC-TS            PIC X(26).
           05  TRAN-DETAILS            PIC X(200).
           05  TRAN-DEBIT-DETAIL REDEFINES TRAN-DETAILS.
               10  DEBIT-REASON        PIC X(50).
               10  DEBIT-AUTH-CODE     PIC X(20).
               10  FILLER              PIC X(130).
           05  TRAN-CREDIT-DETAIL REDEFINES TRAN-DETAILS.
               10  CREDIT-SOURCE       PIC X(50).
               10  CREDIT-REF-NUM      PIC X(30).
               10  FILLER              PIC X(120).
```

### Java POJO (Inheritance Strategy)

```java
package com.carddemo.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tran_type_cd", discriminatorType = DiscriminatorType.STRING)
public abstract class Transaction {
    
    @Id
    @Column(name = "tran_id", length = 16)
    private String transactionId;
    
    @Column(name = "tran_type_cd", length = 2, insertable = false, updatable = false)
    private String transactionTypeCode;
    
    @Column(name = "tran_cat_cd")
    private Integer categoryCode;
    
    @Column(name = "tran_source", length = 10)
    private String source;
    
    @Column(name = "tran_desc", length = 100)
    private String description;
    
    @Column(name = "tran_amt", precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "tran_card_num", length = 16)
    private String cardNumber;
    
    @Column(name = "merchant_id")
    private Long merchantId;
    
    @Column(name = "merchant_name", length = 50)
    private String merchantName;
    
    @Column(name = "merchant_city", length = 50)
    private String merchantCity;
    
    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;
    
    @Column(name = "orig_timestamp")
    private LocalDateTime originalTimestamp;
    
    @Column(name = "proc_timestamp")
    private LocalDateTime processedTimestamp;
    
    // Getters, setters, constructors
}

@Entity
@DiscriminatorValue("DB")
public class DebitTransaction extends Transaction {
    
    @Column(name = "debit_reason", length = 50)
    private String debitReason;  // From TRAN-DEBIT-DETAIL
    
    @Column(name = "debit_auth_code", length = 20)
    private String authorizationCode;  // From TRAN-DEBIT-DETAIL
    
    // Getters, setters
}

@Entity
@DiscriminatorValue("CR")
public class CreditTransaction extends Transaction {
    
    @Column(name = "credit_source", length = 50)
    private String creditSource;  // From TRAN-CREDIT-DETAIL
    
    @Column(name = "credit_ref_num", length = 30)
    private String referenceNumber;  // From TRAN-CREDIT-DETAIL
    
    // Getters, setters
}

@Entity
@DiscriminatorValue("PY")
public class PaymentTransaction extends Transaction {
    // Payment-specific fields
}
```

## Data Type Mapping Summary

| COBOL Type | COBOL Example | Java Type | JPA Annotation |
|------------|---------------|-----------|----------------|
| `PIC 9(n) COMP` | `PIC 9(09) COMP` | `Long` | `@Column` |
| `PIC X(n)` | `PIC X(25)` | `String` | `@Column(length=25)` |
| `PIC S9(n)V99 COMP-3` | `PIC S9(13)V99 COMP-3` | `BigDecimal` | `@Column(precision=15, scale=2)` |
| `OCCURS n TIMES` | `OCCURS 12 TIMES` | `List<T>` | `@OneToMany` |
| `88 level` | `88 STATUS-ACTIVE VALUE 'A'` | `boolean` method | Custom getter |
| `REDEFINES` | Multiple structures | Inheritance or JSON | `@Inheritance` or `@Column(columnDefinition="jsonb")` |
| Date (YYYYMMDD) | `PIC X(10)` | `LocalDate` | `@Column` |
| Timestamp | `PIC X(26)` | `LocalDateTime` | `@Column` |

## Tools for Automated Conversion

### JRecord (GitHub: bmTas/JRecord)
```bash
# Generate POJOs from copybook
java -jar jrecord-codegen.jar \
  --copybook CVCUS01Y.cpy \
  --output Customer.java \
  --package com.carddemo.model
```

### cb2java (GitHub: JulianSauer/cb2java)
```java
import net.sf.cb2java.copybook.Copybook;
import net.sf.cb2java.copybook.CopybookParser;

Copybook copybook = CopybookParser.parse("CVCUS01Y", 
    new FileInputStream("CVCUS01Y.cpy"));
// Use copybook to parse data
```

## Conversion Checklist

- [ ] All COMP-3 fields mapped to BigDecimal
- [ ] String lengths preserved
- [ ] OCCURS converted to collections or child tables
- [ ] REDEFINES strategy selected
- [ ] 88-level conditions converted to boolean methods
- [ ] Date/timestamp conversions implemented
- [ ] Group items converted to @Embeddable or separate entities
- [ ] JPA annotations added
- [ ] Validation constraints added
- [ ] Helper methods created
- [ ] Unit tests written

## Testing Data Conversion

```java
@Test
void testCobolToJavaConversion() {
    // Given: COBOL fixed-length record
    String cobolRecord = 
        "000123456" +  // CUST-ID (9 digits)
        "JOHN                     " +  // FIRST-NAME (25 chars)
        "DOE                      " +  // LAST-NAME (25 chars)
        // ... rest of fields
    
    // When: Parse and convert
    Customer customer = parseCobolRecord(cobolRecord);
    
    // Then: Verify conversion
    assertEquals(123456L, customer.getCustId());
    assertEquals("JOHN", customer.getFirstName());
    assertEquals("DOE", customer.getLastName());
}
```
