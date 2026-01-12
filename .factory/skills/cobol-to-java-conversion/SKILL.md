---
name: cobol-to-java-conversion
description: Convert COBOL applications to Java following mainframe modernization best practices. Handles CICS transactions, batch programs, VSAM files, DB2 integration, and data structure migrations. Use when modernizing CardDemo COBOL code to cloud-native Java applications.
tags: [cobol, java, modernization, aws-transform, mainframe, cics, vsam]
---

# COBOL to Java Conversion

## Overview

Comprehensive guide for converting COBOL mainframe applications to modern Java, preserving business logic while adopting cloud-native architecture patterns. This skill is specifically tailored for the CardDemo application but provides general best practices applicable to any COBOL modernization effort.

## When to Use This Skill

- Converting COBOL online CICS programs to Java Spring Boot REST services
- Migrating COBOL batch programs to Java scheduled jobs or Spring Batch
- Translating COBOL copybooks to Java POJOs
- Migrating VSAM files to SQL databases
- Modernizing DB2/IMS data access to JPA/JDBC

## Prerequisites

### Knowledge Required
- Understanding of COBOL syntax and mainframe concepts (CICS, VSAM, JCL)
- Java development experience (Spring Boot, JPA, REST APIs)
- Database knowledge (SQL, transactions, data modeling)

### Tools Required
- **AWS Transform** (optional but recommended for automated analysis)
- **IDE:** IntelliJ IDEA or Eclipse with COBOL plugin
- **Build Tools:** Maven or Gradle
- **Database:** PostgreSQL, MySQL, or Oracle
- **Testing:** JUnit 5, Mockito, Spring Test

### Environment Setup
1. Clone this repository
2. Review `mappings/*.md` files for conversion patterns
3. Install copybook-to-POJO converters (see Tools section)
4. Set up local database for testing

## Conversion Workflow

### Phase 1: Analysis & Planning

**1.1 Analyze COBOL Program**

```bash
# Run analysis script
./scripts/analyze-cobol.sh app/cbl/COSGN00C.cbl
```

**Output includes:**
- Program dependencies (COPY statements, CALL statements)
- File I/O operations (VSAM, DB2)
- CICS commands (SEND MAP, RECEIVE, LINK, etc.)
- Complex data structures (REDEFINES, OCCURS)

**1.2 Identify Conversion Pattern**

Refer to decision tree:

| COBOL Type | Conversion Target | Reference |
|------------|------------------|-----------|
| CICS Online Transaction | Spring Boot REST Controller | `mappings/cics-to-spring.md` |
| Batch Program (Sequential) | Spring Batch Job | `examples/batch-conversion-example.md` |
| Copybook | Java POJO / Record | `examples/copybook-conversion-example.md` |
| VSAM KSDS | SQL Table with Primary Key | `mappings/vsam-to-sql.md` |
| DB2 Embedded SQL | JPA Entity / JDBC Template | `mappings/data-types.md` |

**1.3 Review Pre-Conversion Checklist**
- [ ] Business logic documented
- [ ] All dependencies identified
- [ ] Test data prepared
- [ ] Conversion pattern selected
- [ ] Target Java architecture designed

See `checklists/pre-conversion.md` for complete checklist.

### Phase 2: Data Structure Conversion

**2.1 Convert Copybooks to POJOs**

**COBOL Copybook Example:**
```cobol
01  CUSTOMER-RECORD.
    05  CUST-ID           PIC 9(10).
    05  CUST-NAME         PIC X(50).
    05  CUST-BALANCE      PIC S9(13)V99 COMP-3.
    05  CUST-STATUS       PIC X(01).
```

**Java POJO (Converted):**
```java
@Entity
@Table(name = "CUSTOMER")
public class Customer {
    @Id
    private Long custId;
    
    @Column(length = 50)
    private String custName;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal custBalance;  // Note: BigDecimal for COMP-3
    
    private String custStatus;
    
    // Getters, setters, constructors
}
```

**⚠️ Critical: Data Type Mappings**

| COBOL Type | Java Type | Notes |
|------------|-----------|-------|
| `PIC 9(n)` | `Long` or `Integer` | Use Long for n > 9 |
| `PIC X(n)` | `String` | Trim trailing spaces |
| `PIC S9(n)V99 COMP-3` | `BigDecimal` | **Always use BigDecimal for financial data** |
| `PIC S9(n) COMP` | `Integer` or `Long` | Binary integer |
| `PIC S9(n) COMP-5` | `Long` | Binary long |

See `mappings/data-types.md` for complete table.

### Phase 3: Business Logic Conversion

**3.1 CICS Transaction → Spring Boot REST Service**

**COBOL CICS Program Structure:**
```cobol
IDENTIFICATION DIVISION.
PROGRAM-ID. COSGN00C.
*> Signon transaction

WORKING-STORAGE SECTION.
01  WS-USER-ID     PIC X(08).
01  WS-USER-PWD    PIC X(08).

PROCEDURE DIVISION.
    EXEC CICS RECEIVE MAP('COSGN0A')
              MAPSET('COSGN00')
              INTO(WS-MAP-AREA)
    END-EXEC.
    
    *> Validate user credentials
    PERFORM VALIDATE-USER.
    
    *> Read security file
    EXEC CICS READ FILE('USRSEC')
              RIDFLD(WS-USER-ID)
              INTO(WS-USER-RECORD)
    END-EXEC.
```

**Converted Java Spring Boot:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    @Autowired
    private UserSecurityService userSecurityService;
    
    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(
            @RequestBody @Valid SigninRequest request) {
        
        // Validate user credentials (business logic preserved)
        User user = userSecurityService.validateUser(
            request.getUserId(), 
            request.getPassword()
        );
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SigninResponse("Invalid credentials"));
        }
        
        // Generate token (modern approach vs CICS commarea)
        String token = jwtService.generateToken(user);
        
        return ResponseEntity.ok(
            new SigninResponse(token, user)
        );
    }
}
```

**Key Conversion Patterns:**
- **CICS RECEIVE MAP** → `@PostMapping` with `@RequestBody`
- **CICS SEND MAP** → `ResponseEntity` with JSON
- **CICS FILE READ** → JPA repository `findById()`
- **CICS LINK** → Service method call or REST client
- **COMMAREA** → DTOs passed between methods

See `mappings/cics-to-spring.md` for complete patterns.

**3.2 Batch Program → Spring Batch**

**COBOL Batch Structure:**
```cobol
PROGRAM-ID. CBTRN02C.
*> Post daily transactions

PROCEDURE DIVISION.
    OPEN INPUT DALYTRAN-FILE
    OPEN I-O TRANSACT-FILE
    
    PERFORM UNTIL EOF
        READ DALYTRAN-FILE
        PERFORM PROCESS-TRANSACTION
        WRITE TRANSACT-FILE FROM WS-TRANSACTION
    END-PERFORM.
```

**Java Spring Batch:**
```java
@Configuration
public class TransactionPostingBatchJob {
    
    @Bean
    public Job postTransactionsJob(JobRepository jobRepository,
                                   Step processTransactionStep) {
        return new JobBuilder("postTransactions", jobRepository)
            .start(processTransactionStep)
            .build();
    }
    
    @Bean
    public Step processTransactionStep(
            JobRepository jobRepository,
            PlatformTransactionManager txManager,
            ItemReader<DailyTransaction> reader,
            ItemProcessor<DailyTransaction, Transaction> processor,
            ItemWriter<Transaction> writer) {
        return new StepBuilder("processTransactionStep", jobRepository)
            .<DailyTransaction, Transaction>chunk(100, txManager)
            .reader(reader)    // Read from staging table
            .processor(processor) // Business logic
            .writer(writer)    // Write to main table
            .build();
    }
}
```

See `examples/batch-conversion-example.md` for complete example.

### Phase 4: Data Migration

**4.1 VSAM to SQL**

Refer to `mappings/vsam-to-sql.md` for:
- Schema design patterns
- Index strategies (KSDS primary key → SQL PRIMARY KEY, AIX → SQL INDEX)
- Data extraction scripts
- Bulk load strategies

**4.2 Data Type Transformations**

```bash
# Use provided script for data migration
python3 scripts/generate-test-data.py \
  --copybook app/cpy/CUSTOMER.cpy \
  --output test-data.json \
  --records 1000
```

See `checklists/data-migration.md` for complete process.

### Phase 5: Testing & Validation

**5.1 Unit Testing**

Create equivalent test cases:
```java
@SpringBootTest
class SigninServiceTest {
    
    @Test
    void testValidCredentials_Success() {
        // Given
        String userId = "USER0001";
        String password = "PASSWORD";
        
        // When
        User user = signinService.authenticate(userId, password);
        
        // Then
        assertNotNull(user);
        assertEquals(userId, user.getUserId());
    }
    
    @Test
    void testInvalidPassword_ThrowsException() {
        // Test invalid credentials
        assertThrows(InvalidCredentialsException.class, () -> {
            signinService.authenticate("USER0001", "WRONG");
        });
    }
}
```

**5.2 Integration Testing**

Use `TestRestTemplate` to test full flow:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SigninControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testSigninFlow_EndToEnd() {
        SigninRequest request = new SigninRequest("USER0001", "PASSWORD");
        
        ResponseEntity<SigninResponse> response = restTemplate
            .postForEntity("/api/auth/signin", request, SigninResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getToken());
    }
}
```

**5.3 Data Validation**

Compare COBOL output vs Java output using provided scripts.

See `checklists/post-conversion-validation.md` for complete checklist.

## Common Pitfalls & Solutions

### 1. Numeric Precision Loss

**❌ Problem:**
```java
double balance = 123.45;  // Can cause rounding errors
```

**✅ Solution:**
```java
BigDecimal balance = new BigDecimal("123.45");  // Precise decimal
```

### 2. JOBOL Anti-Pattern

**❌ Problem:** Direct line-by-line translation
```java
// Bad: Procedural COBOL-style Java
public class SignonProgram {
    public void main() {
        receiveMap();
        if (eibaid == DFHENTER) {
            processEnterKey();
        }
    }
}
```

**✅ Solution:** Object-oriented Java
```java
@RestController
public class AuthenticationController {
    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(@RequestBody SigninRequest req) {
        // Clean, RESTful design
    }
}
```

### 3. Ignoring CICS Transaction Context

**⚠️ Remember:** CICS is pseudo-conversational. Don't try to maintain server-side state.

**✅ Solution:** Use JWT tokens or session management

### 4. EBCDIC Encoding Issues

**✅ Solution:** Always specify encoding when reading mainframe data:
```java
String data = new String(bytes, Charset.forName("IBM037")); // EBCDIC
```

See `references/common-pitfalls.md` for complete list.

## AWS Transform Integration

### Using AWS Transform for Automated Analysis

```bash
# Upload COBOL source to S3
aws s3 cp app/cbl/ s3://my-bucket/cobol-source/ --recursive

# Start AWS Transform project
aws transform create-project \
  --name carddemo-modernization \
  --source-location s3://my-bucket/cobol-source/

# Get analysis results
aws transform get-analysis --project-id <project-id>
```

AWS Transform provides:
- Dependency graphs
- Complexity analysis
- Automated refactoring suggestions
- Test case generation

See `references/aws-transform-guide.md` for setup instructions.

## Tools & Resources

### Recommended Tools

1. **Copybook Converters:**
   - `cobol2java-copybook-converter` (GitHub: manuelscurti/cobol2java-copybook-converter)
   - `JRecord` (GitHub: bmTas/JRecord)
   - `cb2java` (Dynamic parser)

2. **Testing:**
   - JUnit 5
   - Spring Test
   - TestContainers (for database testing)

3. **AWS Services:**
   - AWS Transform
   - AWS Mainframe Modernization
   - AWS Blu Age

### Reference Documentation

- [AWS Mainframe Modernization](https://aws.amazon.com/mainframe-modernization/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Batch](https://spring.io/projects/spring-batch)
- COBOL Language Reference (IBM)

## Verification Checklist

Before marking conversion complete:

- [ ] All business logic tests pass
- [ ] Performance meets or exceeds COBOL baseline
- [ ] Data integrity validated (checksums, record counts)
- [ ] Error handling implemented
- [ ] Logging added for troubleshooting
- [ ] Security reviewed (authentication, authorization)
- [ ] Documentation updated
- [ ] Code review completed
- [ ] Integration tests pass
- [ ] Deployment tested in staging

## Examples

See `examples/` directory for:
- Complete CICS-to-Spring conversion
- Batch program migration
- Copybook-to-POJO examples

## Iterative Approach

**Recommended order for CardDemo conversion:**

1. **Phase 1:** Data structures (copybooks → POJOs)
2. **Phase 2:** Simple CICS transactions (COSGN00C signon)
3. **Phase 3:** Complex transactions (account updates, transactions)
4. **Phase 4:** Batch programs (statement generation)
5. **Phase 5:** Optional modules (DB2, IMS, MQ)

Each phase allows testing and validation before proceeding.

## Support & Collaboration

- Review existing sessions for patterns discovered by team
- Document new patterns in this skill as they emerge
- Update mappings when edge cases are found
- Share learnings via pull requests

## Maintenance

This skill should be updated as:
- New COBOL patterns are discovered
- Java best practices evolve
- AWS Transform capabilities expand
- Team identifies additional pitfalls
