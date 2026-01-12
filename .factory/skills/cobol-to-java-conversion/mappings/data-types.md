# COBOL to Java Data Type Mappings

Complete reference for converting COBOL data types to appropriate Java types.

## Basic Data Types

| COBOL Type | COBOL Example | Java Type | Java Example | Notes |
|------------|---------------|-----------|--------------|-------|
| `PIC 9(n)` | `PIC 9(5)` | `Integer` | `Integer amount` | For n ≤ 9 |
| `PIC 9(n)` | `PIC 9(10)` | `Long` | `Long accountId` | For n > 9 |
| `PIC X(n)` | `PIC X(50)` | `String` | `String name` | Alphanumeric |
| `PIC A(n)` | `PIC A(30)` | `String` | `String description` | Alphabetic only |
| `PIC S9(n)` | `PIC S9(7)` | `Integer` | `Integer signedValue` | Signed integer |

## Numeric Types with Decimals

| COBOL Type | COBOL Example | Java Type | Java Example | Notes |
|------------|---------------|-----------|--------------|-------|
| `PIC 9(n)V9(m)` | `PIC 9(10)V99` | `BigDecimal` | `BigDecimal amount` | **Recommended for all financial data** |
| `PIC S9(n)V99` | `PIC S9(13)V99` | `BigDecimal` | `BigDecimal balance` | Signed with 2 decimal places |
| `PIC S9(n)V9(m)` | `PIC S9(7)V9(4)` | `BigDecimal` | `BigDecimal rate` | Variable decimal places |

**⚠️ Critical:** Always use `BigDecimal` for financial calculations to avoid floating-point precision errors.

## COMP Types (Binary/Packed Decimal)

| COBOL Type | COBOL Example | Java Type | Java Example | Notes |
|------------|---------------|-----------|--------------|-------|
| `PIC S9(n) COMP` | `PIC S9(4) COMP` | `Short` | `short quantity` | 16-bit binary |
| `PIC S9(n) COMP` | `PIC S9(9) COMP` | `Integer` | `int counter` | 32-bit binary |
| `PIC S9(n) COMP` | `PIC S9(18) COMP` | `Long` | `long bigNumber` | 64-bit binary |
| `PIC S9(n)V99 COMP-3` | `PIC S9(13)V99 COMP-3` | `BigDecimal` | `BigDecimal balance` | **Packed decimal - use BigDecimal** |
| `PIC 9(n) COMP-5` | `PIC 9(9) COMP-5` | `Integer` | `int nativeInt` | Native binary integer |
| `PIC 9(n) COMP-5` | `PIC 9(18) COMP-5` | `Long` | `long nativeLong` | Native binary long |

### COMP-3 Conversion Example

**COBOL:**
```cobol
01  ACCOUNT-BALANCE     PIC S9(13)V99 COMP-3.
```

**Java:**
```java
@Column(precision = 15, scale = 2)
private BigDecimal accountBalance;
```

**Important:** COMP-3 (packed decimal) stores decimal digits efficiently. In Java, always use `BigDecimal` to maintain precision.

## Date and Time Types

| COBOL Type | COBOL Example | Java Type | Java Example | Notes |
|------------|---------------|-----------|--------------|-------|
| `PIC 9(8)` | `PIC 9(8)` (YYYYMMDD) | `LocalDate` | `LocalDate transDate` | Convert from YYYYMMDD format |
| `PIC 9(6)` | `PIC 9(6)` (YYMMDD) | `LocalDate` | `LocalDate date` | 2-digit year |
| `PIC 9(6)` | `PIC 9(6)` (HHMMSS) | `LocalTime` | `LocalTime time` | Time only |
| `PIC 9(14)` | `PIC 9(14)` (YYYYMMDDHHMMSS) | `LocalDateTime` | `LocalDateTime timestamp` | Full timestamp |

### Date Conversion Examples

**COBOL YYYYMMDD to Java LocalDate:**
```java
public LocalDate cobolDateToLocalDate(int cobolDate) {
    int year = cobolDate / 10000;
    int month = (cobolDate % 10000) / 100;
    int day = cobolDate % 100;
    return LocalDate.of(year, month, day);
}
```

**Java LocalDate to COBOL YYYYMMDD:**
```java
public int localDateToCobolDate(LocalDate date) {
    return date.getYear() * 10000 + 
           date.getMonthValue() * 100 + 
           date.getDayOfMonth();
}
```

## Boolean/Flag Fields

| COBOL Type | COBOL Example | Java Type | Java Example | Notes |
|------------|---------------|-----------|--------------|-------|
| `PIC X(01)` | `88 ACTIVE VALUE 'Y'` | `Boolean` | `boolean isActive` | Convert 'Y'/'N' to true/false |
| `PIC X(01)` | `88 STATUS-OK VALUE '0'` | `Boolean` | `boolean statusOk` | Convert '0'/'1' or other values |

### Boolean Conversion Example

**COBOL:**
```cobol
01  STATUS-FLAG         PIC X(01).
    88  STATUS-ACTIVE   VALUE 'Y'.
    88  STATUS-INACTIVE VALUE 'N'.
```

**Java:**
```java
private Boolean active;  // true if 'Y', false if 'N'

public void setActiveFromCobol(String flag) {
    this.active = "Y".equals(flag);
}

public String getActiveForCobol() {
    return active ? "Y" : "N";
}
```

## Array Types (OCCURS)

| COBOL Type | COBOL Example | Java Type | Java Example | Notes |
|------------|---------------|-----------|--------------|-------|
| `OCCURS n TIMES` | `PIC X(10) OCCURS 12` | `String[]` | `String[] months` | Fixed-size array |
| `OCCURS n TIMES` | `PIC 9(5) OCCURS 100` | `Integer[]` or `List<Integer>` | `List<Integer> values` | Consider List for flexibility |
| `OCCURS ... DEPENDING ON` | Variable-length array | `List<T>` | `List<Transaction> items` | Dynamic size |

### Array Conversion Example

**COBOL:**
```cobol
01  MONTHLY-DATA.
    05  MONTH-AMOUNT    PIC S9(11)V99 COMP-3 OCCURS 12 TIMES.
```

**Java:**
```java
@ElementCollection
@CollectionTable(name = "monthly_amounts")
private List<BigDecimal> monthlyAmounts = new ArrayList<>(12);
```

## Group Items / Structures (REDEFINES)

### Simple Group Item

**COBOL:**
```cobol
01  CUSTOMER-RECORD.
    05  CUST-ID         PIC 9(10).
    05  CUST-NAME       PIC X(50).
    05  CUST-ADDRESS.
        10  ADDR-LINE1  PIC X(40).
        10  ADDR-LINE2  PIC X(40).
        10  CITY        PIC X(30).
        10  STATE       PIC X(2).
        10  ZIP         PIC X(10).
```

**Java:**
```java
public class Customer {
    private Long custId;
    private String custName;
    
    @Embedded
    private Address address;
}

@Embeddable
public class Address {
    private String addrLine1;
    private String addrLine2;
    private String city;
    private String state;
    private String zip;
}
```

### REDEFINES (Union Types)

**COBOL:**
```cobol
01  TRANSACTION-RECORD.
    05  TRAN-ID         PIC X(16).
    05  TRAN-TYPE       PIC X(02).
    05  TRAN-DATA       PIC X(200).
    05  TRAN-DETAIL REDEFINES TRAN-DATA.
        10  DEBIT-INFO.
            15  DEBIT-AMOUNT    PIC S9(13)V99 COMP-3.
            15  DEBIT-ACCOUNT   PIC X(16).
            15  FILLER          PIC X(171).
        10  CREDIT-INFO REDEFINES DEBIT-INFO.
            15  CREDIT-AMOUNT   PIC S9(13)V99 COMP-3.
            15  CREDIT-ACCOUNT  PIC X(16).
            15  FILLER          PIC X(171).
```

**Java Option 1: Separate Classes with Discriminator**
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tran_type")
public abstract class Transaction {
    private String tranId;
    private String tranType;
}

@Entity
@DiscriminatorValue("DB")
public class DebitTransaction extends Transaction {
    private BigDecimal debitAmount;
    private String debitAccount;
}

@Entity
@DiscriminatorValue("CR")
public class CreditTransaction extends Transaction {
    private BigDecimal creditAmount;
    private String creditAccount;
}
```

**Java Option 2: Store as JSON**
```java
@Entity
public class Transaction {
    private String tranId;
    private String tranType;
    
    @Column(columnDefinition = "jsonb")
    private String tranData;  // Store varying structure as JSON
}
```

## Special Cases

### Zoned Decimal

**COBOL:**
```cobol
01  ZONED-NUMBER        PIC S9(7).  *> No COMP, zoned decimal
```

**Java:**
```java
private Integer zonedNumber;  // Convert from zoned to binary
```

### Editing Characters (Display Format)

**COBOL:**
```cobol
01  DISPLAY-AMOUNT      PIC $$$,$$9.99.
```

**Java:**
```java
private BigDecimal amount;  // Store raw value

// Format for display
DecimalFormat formatter = new DecimalFormat("$###,##0.00");
String displayAmount = formatter.format(amount);
```

### National Characters (Unicode)

**COBOL:**
```cobol
01  UNICODE-NAME        PIC N(50).
```

**Java:**
```java
private String unicodeName;  // Java String is already Unicode
```

## NULL Handling

COBOL doesn't have true NULL concept. Common patterns:

| COBOL Convention | Java Equivalent |
|------------------|-----------------|
| Spaces (`' '`) for strings | `null` or empty string `""` |
| Zeros (`0`) for numbers | `null` or `0` (depending on business logic) |
| High-values | `null` |
| Low-values | `null` |

### NULL Conversion Strategy

```java
public String cobolStringToJava(String cobolString) {
    if (cobolString == null || cobolString.trim().isEmpty()) {
        return null;  // or "" depending on requirements
    }
    return cobolString.trim();
}

public Integer cobolNumberToJava(int cobolNumber) {
    if (cobolNumber == 0) {
        return null;  // Only if 0 means NULL in business logic
    }
    return cobolNumber;
}
```

## Common Pitfalls

### ❌ DON'T: Use double or float for financial data
```java
double balance = 123.45;  // WRONG - precision loss
```

### ✅ DO: Use BigDecimal for financial data
```java
BigDecimal balance = new BigDecimal("123.45");  // CORRECT
```

### ❌ DON'T: Forget to trim COBOL strings
```java
String name = cobolRecord.getName();  // May have trailing spaces
```

### ✅ DO: Trim strings from COBOL
```java
String name = cobolRecord.getName().trim();
```

### ❌ DON'T: Lose leading zeros
```java
String accountNumber = String.valueOf(123);  // "123" - lost leading zeros
```

### ✅ DO: Preserve leading zeros if needed
```java
String accountNumber = String.format("%010d", 123);  // "0000000123"
```

## JPA Annotations for Precision

```java
@Entity
@Table(name = "ACCOUNT")
public class Account {
    
    @Id
    @Column(length = 16, nullable = false)
    private String accountId;
    
    @Column(length = 50, nullable = false)
    private String accountName;
    
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance;  // COMP-3 equivalent
    
    @Column(length = 1)
    private String status;  // 'A' or 'I'
    
    @Column(name = "open_date")
    private LocalDate openDate;
}
```

## Validation

When converting, always validate:
- Numeric ranges (especially for `Integer` vs `Long`)
- String lengths
- Decimal precision and scale
- Date ranges (COBOL dates can have invalid values)
- NULL handling matches business rules

## Testing Data Type Conversions

```java
@Test
void testComp3ToBigDecimal() {
    // Given
    BigDecimal cobolValue = new BigDecimal("12345678901.23");
    
    // When
    BigDecimal javaValue = convertComp3ToJava(cobolValue);
    
    // Then
    assertEquals(0, cobolValue.compareTo(javaValue));
    assertEquals(2, javaValue.scale());
}
```
