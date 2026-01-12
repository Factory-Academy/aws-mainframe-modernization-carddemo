# CICS to Spring Boot Mapping Patterns

Complete guide for converting CICS COBOL programs to Spring Boot REST services.

## CICS Command Mappings

### File I/O Operations

| CICS Command | Spring Boot Equivalent | Example |
|--------------|----------------------|---------|
| `EXEC CICS READ` | JPA `findById()` | See below |
| `EXEC CICS WRITE` | JPA `save()` | See below |
| `EXEC CICS REWRITE` | JPA `save()` (existing entity) | See below |
| `EXEC CICS DELETE` | JPA `deleteById()` | See below |
| `EXEC CICS STARTBR` | JPA query with pagination | See below |
| `EXEC CICS READNEXT` | Iterate query results | See below |
| `EXEC CICS ENDBR` | Close iterator | See below |

#### READ Example

**COBOL CICS:**
```cobol
EXEC CICS READ
    FILE('ACCTDAT')
    RIDFLD(WS-ACCOUNT-ID)
    INTO(WS-ACCOUNT-RECORD)
    RESP(WS-RESP-CD)
END-EXEC.

IF WS-RESP-CD = DFHRESP(NOTFND)
    MOVE 'Account not found' TO WS-MESSAGE
END-IF.
```

**Java Spring Boot:**
```java
@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;
    
    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }
}

// Repository
public interface AccountRepository extends JpaRepository<Account, String> {
}
```

#### WRITE Example

**COBOL CICS:**
```cobol
EXEC CICS WRITE
    FILE('ACCTDAT')
    FROM(WS-ACCOUNT-RECORD)
    RIDFLD(WS-ACCOUNT-ID)
    RESP(WS-RESP-CD)
END-EXEC.

IF WS-RESP-CD = DFHRESP(DUPREC)
    MOVE 'Duplicate account' TO WS-MESSAGE
END-IF.
```

**Java Spring Boot:**
```java
@Service
public class AccountService {
    public Account createAccount(Account account) {
        if (accountRepository.existsById(account.getAccountId())) {
            throw new DuplicateAccountException("Account already exists");
        }
        return accountRepository.save(account);
    }
}
```

#### BROWSE (STARTBR/READNEXT) Example

**COBOL CICS:**
```cobol
EXEC CICS STARTBR
    FILE('ACCTDAT')
    RIDFLD(WS-START-KEY)
    RESP(WS-RESP-CD)
END-EXEC.

PERFORM UNTIL WS-END-OF-FILE
    EXEC CICS READNEXT
        FILE('ACCTDAT')
        INTO(WS-ACCOUNT-RECORD)
        RIDFLD(WS-ACCOUNT-ID)
        RESP(WS-RESP-CD)
    END-EXEC
    
    IF WS-RESP-CD = DFHRESP(ENDFILE)
        SET WS-END-OF-FILE TO TRUE
    ELSE
        PERFORM PROCESS-ACCOUNT
    END-IF
END-PERFORM.

EXEC CICS ENDBR
    FILE('ACCTDAT')
END-EXEC.
```

**Java Spring Boot:**
```java
@Service
public class AccountService {
    public List<Account> getAccountsStartingWith(String prefix) {
        return accountRepository.findByAccountIdStartingWith(prefix);
    }
    
    // Or with pagination
    public Page<Account> getAccounts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return accountRepository.findAll(pageable);
    }
}
```

### Screen/UI Operations

| CICS Command | Spring Boot Equivalent | Notes |
|--------------|----------------------|-------|
| `EXEC CICS SEND MAP` | `ResponseEntity` with JSON | Return response to client |
| `EXEC CICS RECEIVE MAP` | `@RequestBody` | Receive request from client |
| `EXEC CICS SEND TEXT` | `ResponseEntity<String>` | Plain text response |

#### MAP SEND/RECEIVE Example

**COBOL CICS:**
```cobol
PROCEDURE DIVISION.
    EXEC CICS RECEIVE MAP('SIGNIN')
              MAPSET('SIGNMAP')
              INTO(SIGNIN-MAP)
    END-EXEC.
    
    MOVE USERIDI OF SIGNIN-MAP TO WS-USER-ID.
    MOVE PASSWORDI OF SIGNIN-MAP TO WS-PASSWORD.
    
    PERFORM VALIDATE-CREDENTIALS.
    
    IF VALID-USER
        MOVE 'Login successful' TO MESSAGEO OF SIGNIN-MAP
    ELSE
        MOVE 'Invalid credentials' TO MESSAGEO OF SIGNIN-MAP
    END-IF.
    
    EXEC CICS SEND MAP('SIGNIN')
              MAPSET('SIGNMAP')
              FROM(SIGNIN-MAP)
    END-EXEC.
```

**Java Spring Boot:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    @Autowired
    private AuthenticationService authService;
    
    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(
            @RequestBody @Valid SigninRequest request) {
        
        try {
            User user = authService.validateCredentials(
                request.getUserId(), 
                request.getPassword()
            );
            
            String token = jwtService.generateToken(user);
            
            return ResponseEntity.ok(
                new SigninResponse("Login successful", token, user)
            );
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SigninResponse("Invalid credentials", null, null));
        }
    }
}

// DTOs
public record SigninRequest(
    @NotBlank String userId,
    @NotBlank String password
) {}

public record SigninResponse(
    String message,
    String token,
    User user
) {}
```

### Program Control

| CICS Command | Spring Boot Equivalent | Notes |
|--------------|----------------------|-------|
| `EXEC CICS LINK` | Service method call or `@Async` | Synchronous call |
| `EXEC CICS XCTL` | Transfer to another controller | Rare in REST |
| `EXEC CICS RETURN` | Method return | End of processing |
| `EXEC CICS ABEND` | `throw RuntimeException` | Abnormal termination |

#### LINK Example

**COBOL CICS:**
```cobol
MOVE ACCOUNT-ID TO CA-ACCOUNT-ID.
MOVE 'INQUIRY' TO CA-FUNCTION.

EXEC CICS LINK
    PROGRAM('ACCTPROG')
    COMMAREA(CA-DATA)
    LENGTH(CA-LENGTH)
END-EXEC.

MOVE CA-BALANCE TO WS-BALANCE.
```

**Java Spring Boot:**
```java
@Service
public class AccountInquiryService {
    
    @Autowired
    private AccountService accountService;
    
    public AccountInquiryResponse inquireAccount(String accountId) {
        // Direct service call replaces CICS LINK
        Account account = accountService.getAccountDetails(accountId);
        
        return new AccountInquiryResponse(
            account.getAccountId(),
            account.getBalance(),
            account.getStatus()
        );
    }
}
```

### Transaction Control

| CICS Command | Spring Boot Equivalent | Notes |
|--------------|----------------------|-------|
| `EXEC CICS SYNCPOINT` | `@Transactional` | Commit transaction |
| `EXEC CICS SYNCPOINT ROLLBACK` | `throw Exception` in `@Transactional` | Rollback |

**COBOL CICS:**
```cobol
EXEC CICS READ UPDATE
    FILE('ACCTDAT')
    RIDFLD(WS-ACCOUNT-ID)
    INTO(WS-ACCOUNT-RECORD)
END-EXEC.

ADD PAYMENT-AMOUNT TO ACCOUNT-BALANCE.

EXEC CICS REWRITE
    FILE('ACCTDAT')
    FROM(WS-ACCOUNT-RECORD)
END-EXEC.

EXEC CICS SYNCPOINT
END-EXEC.
```

**Java Spring Boot:**
```java
@Service
public class PaymentService {
    
    @Transactional  // Handles commit/rollback automatically
    public void processPayment(String accountId, BigDecimal paymentAmount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException());
        
        account.setBalance(account.getBalance().add(paymentAmount));
        
        accountRepository.save(account);
        
        // Automatic SYNCPOINT on method completion
        // Automatic ROLLBACK if exception thrown
    }
}
```

## COMMAREA → DTO Pattern

CICS uses COMMAREA to pass data between programs. In Spring Boot, use DTOs (Data Transfer Objects).

**COBOL CICS:**
```cobol
01  COMMUNICATION-AREA.
    05  CA-FUNCTION     PIC X(10).
    05  CA-ACCOUNT-ID   PIC X(16).
    05  CA-AMOUNT       PIC S9(13)V99 COMP-3.
    05  CA-STATUS       PIC X(01).
    05  CA-MESSAGE      PIC X(80).
```

**Java Spring Boot:**
```java
public class TransactionRequest {
    private String function;
    private String accountId;
    private BigDecimal amount;
    private String status;
    private String message;
    
    // Getters, setters, constructors
}
```

## Pseudo-Conversational → Stateless REST

CICS programs are often pseudo-conversational (don't hold resources between user interactions). Spring Boot REST APIs are naturally stateless.

**COBOL CICS Pseudo-Conversational:**
```cobol
PROCEDURE DIVISION.
    IF EIBCALEN = 0
        *> First time - initialize
        PERFORM INITIALIZE-SCREEN
    ELSE
        *> Returning from user input
        MOVE DFHCOMMAREA TO WS-COMMAREA
        PERFORM PROCESS-INPUT
    END-IF.
    
    EXEC CICS RETURN
        TRANSID('AC01')
        COMMAREA(WS-COMMAREA)
        LENGTH(WS-COMM-LENGTH)
    END-EXEC.
```

**Java Spring Boot Stateless:**
```java
@RestController
public class AccountController {
    
    // Each request is independent - no state maintained
    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        // Fresh request - no previous context
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(account);
    }
    
    @PostMapping("/accounts/{id}/update")
    public ResponseEntity<Account> updateAccount(
            @PathVariable String id,
            @RequestBody AccountUpdateRequest request) {
        // Each request includes all needed data
        Account account = accountService.updateAccount(id, request);
        return ResponseEntity.ok(account);
    }
}
```

### Maintaining Session State (if needed)

If you need to maintain state across requests:

```java
// Option 1: JWT Token with embedded state
@Service
public class JwtService {
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("accountId", user.getDefaultAccountId());
        // ... other state
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getUserId())
            .signWith(key)
            .compact();
    }
}

// Option 2: Spring Session (database-backed)
@RestController
@SessionAttributes("userContext")
public class AccountController {
    // Spring manages session automatically
}

// Option 3: Redis-backed session
@Configuration
@EnableRedisHttpSession
public class SessionConfig {
    // Session data stored in Redis
}
```

## Error Handling

### CICS Response Codes → HTTP Status Codes

| CICS Response | HTTP Status | Description |
|---------------|-------------|-------------|
| `DFHRESP(NORMAL)` | 200 OK | Success |
| `DFHRESP(NOTFND)` | 404 Not Found | Resource not found |
| `DFHRESP(DUPREC)` | 409 Conflict | Duplicate record |
| `DFHRESP(INVREQ)` | 400 Bad Request | Invalid request |
| `DFHRESP(IOERR)` | 500 Internal Server Error | I/O error |
| `DFHRESP(NOTAUTH)` | 401 Unauthorized | Not authenticated |
| `DFHRESP(NOTOPEN)` | 503 Service Unavailable | Service unavailable |

**COBOL CICS:**
```cobol
EXEC CICS READ
    FILE('ACCTDAT')
    RIDFLD(WS-ACCOUNT-ID)
    INTO(WS-ACCOUNT-RECORD)
    RESP(WS-RESP-CD)
END-EXEC.

EVALUATE WS-RESP-CD
    WHEN DFHRESP(NORMAL)
        PERFORM PROCESS-ACCOUNT
    WHEN DFHRESP(NOTFND)
        MOVE 'Account not found' TO WS-ERROR-MSG
    WHEN DFHRESP(IOERR)
        MOVE 'System error' TO WS-ERROR-MSG
    WHEN OTHER
        MOVE 'Unexpected error' TO WS-ERROR-MSG
END-EVALUATE.
```

**Java Spring Boot:**
```java
@RestController
public class AccountController {
    
    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        try {
            Account account = accountService.getAccount(id);
            return ResponseEntity.ok(account);  // 200 OK
        } catch (AccountNotFoundException e) {
            return ResponseEntity.notFound().build();  // 404 Not Found
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();  // 500 Internal Server Error
        }
    }
}

// Better: Use @ExceptionHandler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateAccountException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Internal server error"));
    }
}
```

## Complete Example: CICS Program → Spring Boot REST API

### COBOL CICS Program

```cobol
IDENTIFICATION DIVISION.
PROGRAM-ID. COACTVWC.
*> Account View Program

WORKING-STORAGE SECTION.
01  WS-ACCOUNT-ID       PIC X(16).
01  WS-ACCOUNT-RECORD.
    05  WS-ACCT-ID      PIC X(16).
    05  WS-ACCT-NAME    PIC X(50).
    05  WS-ACCT-BALANCE PIC S9(13)V99 COMP-3.
    05  WS-ACCT-STATUS  PIC X(01).

PROCEDURE DIVISION.
    EXEC CICS RECEIVE MAP('ACTVWMAP')
              MAPSET('ACCOUNT')
              INTO(ACTVW-MAP)
    END-EXEC.
    
    MOVE ACCTIDIN OF ACTVW-MAP TO WS-ACCOUNT-ID.
    
    EXEC CICS READ
        FILE('ACCTDAT')
        RIDFLD(WS-ACCOUNT-ID)
        INTO(WS-ACCOUNT-RECORD)
        RESP(WS-RESP-CD)
    END-EXEC.
    
    IF WS-RESP-CD = DFHRESP(NORMAL)
        MOVE WS-ACCT-NAME TO NAMEO OF ACTVW-MAP
        MOVE WS-ACCT-BALANCE TO BALANCEO OF ACTVW-MAP
        MOVE WS-ACCT-STATUS TO STATUSO OF ACTVW-MAP
    ELSE
        MOVE 'Account not found' TO MSGO OF ACTVW-MAP
    END-IF.
    
    EXEC CICS SEND MAP('ACTVWMAP')
              MAPSET('ACCOUNT')
              FROM(ACTVW-MAP)
    END-EXEC.
    
    EXEC CICS RETURN
    END-EXEC.
```

### Java Spring Boot Equivalent

```java
// Entity
@Entity
@Table(name = "ACCOUNT")
public class Account {
    @Id
    @Column(length = 16)
    private String accountId;
    
    @Column(length = 50)
    private String accountName;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal balance;
    
    @Column(length = 1)
    private String status;
    
    // Getters, setters, constructors
}

// Repository
public interface AccountRepository extends JpaRepository<Account, String> {
}

// Service
@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;
    
    public Account getAccountView(String accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }
}

// Controller
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountViewResponse> viewAccount(
            @PathVariable String accountId) {
        
        Account account = accountService.getAccountView(accountId);
        
        AccountViewResponse response = new AccountViewResponse(
            account.getAccountId(),
            account.getAccountName(),
            account.getBalance(),
            account.getStatus()
        );
        
        return ResponseEntity.ok(response);
    }
}

// DTO
public record AccountViewResponse(
    String accountId,
    String accountName,
    BigDecimal balance,
    String status
) {}

// Exception Handler
@RestControllerAdvice
public class ExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

## Testing

### Integration Test for REST Endpoint

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AccountControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @BeforeEach
    void setUp() {
        // Create test data
        Account account = new Account();
        account.setAccountId("1234567890123456");
        account.setAccountName("Test Account");
        account.setBalance(new BigDecimal("1000.00"));
        account.setStatus("A");
        accountRepository.save(account);
    }
    
    @Test
    void testViewAccount_Success() {
        String url = "/api/accounts/1234567890123456";
        
        ResponseEntity<AccountViewResponse> response =
            restTemplate.getForEntity(url, AccountViewResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Account", response.getBody().accountName());
        assertEquals(new BigDecimal("1000.00"), response.getBody().balance());
    }
    
    @Test
    void testViewAccount_NotFound() {
        String url = "/api/accounts/9999999999999999";
        
        ResponseEntity<ErrorResponse> response =
            restTemplate.getForEntity(url, ErrorResponse.class);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

## Summary

Key conversion patterns:
1. **CICS FILE commands → JPA repository methods**
2. **SEND/RECEIVE MAP → REST endpoints with JSON**
3. **COMMAREA → DTOs**
4. **Pseudo-conversational → Stateless REST**
5. **CICS responses → HTTP status codes**
6. **LINK/XCTL → Service method calls**
7. **SYNCPOINT → @Transactional**
