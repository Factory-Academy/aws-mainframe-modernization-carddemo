# Common Pitfalls in COBOL to Java Conversion

Quick reference of the most common mistakes and how to avoid them.

## 1. Numeric Precision Loss

**❌ WRONG:**
```java
double balance = 123.45;
float rate = 0.0525f;
```

**✅ CORRECT:**
```java
BigDecimal balance = new BigDecimal("123.45");
BigDecimal rate = new BigDecimal("0.0525");
```

**Why:** Floating-point types (double, float) cannot precisely represent decimal fractions, leading to rounding errors in financial calculations.

## 2. The JOBOL Anti-Pattern

**❌ WRONG:** Procedural COBOL-style Java
```java
public class AccountProgram {
    private String userId;
    private String accountId;
    
    public void mainProcedure() {
        receiveInput();
        validateInput();
        if (errorFlag) {
            sendError();
        } else {
            processAccount();
            sendSuccess();
        }
    }
}
```

**✅ CORRECT:** Object-oriented, idiomatic Java
```java
@RestController
public class AccountController {
    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.create(request));
    }
}
```

## 3. Not Trimming COBOL Strings

**❌ WRONG:**
```java
String name = cobolRecord.getName();  // "JOHN      " (trailing spaces)
if (name.equals("JOHN")) {  // This will fail!
    // ...
}
```

**✅ CORRECT:**
```java
String name = cobolRecord.getName().trim();  // "JOHN"
if (name.equals("JOHN")) {  // This works!
    // ...
}
```

## 4. Losing Leading Zeros

**❌ WRONG:**
```java
int accountId = 123;  // Lost leading zeros from "0000000123"
String formatted = String.valueOf(accountId);  // "123"
```

**✅ CORRECT:**
```java
String accountId = "0000000123";  // Keep as String
// Or format when needed:
String formatted = String.format("%010d", 123);  // "0000000123"
```

## 5. EBCDIC Encoding Issues

**❌ WRONG:**
```java
String data = new String(bytes);  // Uses default charset
```

**✅ CORRECT:**
```java
String data = new String(bytes, Charset.forName("IBM037"));  // EBCDIC
```

## 6. Ignoring CICS Pseudo-Conversational Pattern

**❌ WRONG:** Trying to maintain state on server
```java
@RestController
public class AccountController {
    private String currentAccountId;  // Don't do this!
    
    @GetMapping("/account")
    public Account getAccount() {
        return service.get(currentAccountId);  // Not thread-safe!
    }
}
```

**✅ CORRECT:** Stateless design
```java
@RestController
public class AccountController {
    @GetMapping("/accounts/{id}")
    public Account getAccount(@PathVariable String id) {
        return service.get(id);  // Each request independent
    }
}
```

## 7. Wrong Data Type for COMP-3

**❌ WRONG:**
```java
float balance;  // COBOL: PIC S9(13)V99 COMP-3
```

**✅ CORRECT:**
```java
BigDecimal balance;  // Precise decimal arithmetic
```

## 8. Not Handling COBOL NULL Conventions

**❌ WRONG:**
```java
String name = cobolRecord.getName();  // Might be all spaces
if (name == null) {  // Never true for COBOL data!
    // ...
}
```

**✅ CORRECT:**
```java
String name = cobolRecord.getName();
if (name == null || name.trim().isEmpty()) {  // Handles both null and spaces
    // ...
}
```

## 9. Forgetting Transaction Boundaries

**❌ WRONG:**
```java
public void updateAccount(String id, BigDecimal amount) {
    Account account = repository.findById(id).get();
    account.setBalance(account.getBalance().add(amount));
    repository.save(account);
    
    // If exception occurs here, account is already updated!
    auditService.logUpdate(id, amount);
}
```

**✅ CORRECT:**
```java
@Transactional  // All or nothing
public void updateAccount(String id, BigDecimal amount) {
    Account account = repository.findById(id).get();
    account.setBalance(account.getBalance().add(amount));
    repository.save(account);
    auditService.logUpdate(id, amount);
}
```

## 10. Direct COBOL-to-Java Statement Translation

**❌ WRONG:**
```java
if (responseCode == DFHRESP_NORMAL) {
    if (accountStatus == 'A') {
        if (balance > 0) {
            // Deeply nested procedural code
        }
    }
}
```

**✅ CORRECT:**
```java
if (account.isActive() && account.hasPositiveBalance()) {
    // Clean, expressive OOP
}
```

## 11. Not Validating Date Conversions

**❌ WRONG:**
```java
int cobolDate = 20991301;  // Invalid date!
int year = cobolDate / 10000;  // 2099
int month = (cobolDate % 10000) / 100;  // 13 (invalid!)
int day = cobolDate % 100;  // 01
LocalDate date = LocalDate.of(year, month, day);  // Exception!
```

**✅ CORRECT:**
```java
public LocalDate cobolDateToLocalDate(int cobolDate) {
    int year = cobolDate / 10000;
    int month = (cobolDate % 10000) / 100;
    int day = cobolDate % 100;
    
    try {
        return LocalDate.of(year, month, day);
    } catch (DateTimeException e) {
        throw new InvalidDateException("Invalid COBOL date: " + cobolDate, e);
    }
}
```

## 12. Assuming Java int is Big Enough

**❌ WRONG:**
```java
int accountId;  // COBOL: PIC 9(16)
// Java int max: 2,147,483,647 (10 digits)
// COBOL 9(16): up to 16 digits
```

**✅ CORRECT:**
```java
Long accountId;  // Or keep as String if it's alphanumeric
```

## 13. Not Considering Alternate Index Lookups

**❌ WRONG:**
```java
// Only primary key lookup implemented
Account account = repository.findById(accountId).get();
```

**✅ CORRECT:**
```java
// AIX equivalent: lookup by secondary keys
Account account = repository.findByCustomerId(customerId).get();
Account account = repository.findByCardNumber(cardNumber).get();
```

## 14. Forgetting to Handle REDEFINES

**❌ WRONG:** Treating all fields as if they exist simultaneously
```java
class Transaction {
    BigDecimal debitAmount;
    String debitAccount;
    BigDecimal creditAmount;
    String creditAccount;
    // Both sets of fields populated!
}
```

**✅ CORRECT:** Discriminator-based approach
```java
abstract class Transaction {
    String type;
}

class DebitTransaction extends Transaction {
    BigDecimal debitAmount;
    String debitAccount;
}

class CreditTransaction extends Transaction {
    BigDecimal creditAmount;
    String creditAccount;
}
```

## 15. Not Testing Edge Cases

**❌ WRONG:** Only testing happy path
```java
@Test
void testAccountCreate() {
    Account account = service.create(validRequest);
    assertNotNull(account);
}
```

**✅ CORRECT:** Testing edge cases
```java
@Test
void testAccountCreate_DuplicateId_ThrowsException() {
    assertThrows(DuplicateAccountException.class, () -> {
        service.create(duplicateRequest);
    });
}

@Test
void testAccountCreate_NegativeBalance_ThrowsException() {
    assertThrows(InvalidBalanceException.class, () -> {
        service.create(negativeBalanceRequest);
    });
}
```

## Quick Checklist

Before committing your COBOL-to-Java conversion:

- [ ] All financial calculations use BigDecimal
- [ ] COBOL strings are trimmed
- [ ] Leading zeros preserved where needed
- [ ] EBCDIC encoding handled
- [ ] @Transactional used for multi-step operations
- [ ] Code is object-oriented, not procedural
- [ ] NULL handling accounts for COBOL spaces/zeros
- [ ] Date conversions validated
- [ ] Numeric types sized appropriately
- [ ] REDEFINES strategy implemented
- [ ] Edge cases tested
- [ ] No JOBOL anti-patterns
