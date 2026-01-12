# CICS Program Conversion Example

Complete example of converting a CICS COBOL program to Spring Boot REST API.

## Source: COSGN00C (Signon Program)

### COBOL CICS Program

```cobol
      ******************************************************************
      * Program     : COSGN00C.CBL
      * Function    : Signon Screen for the CardDemo Application
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. COSGN00C.
       
       WORKING-STORAGE SECTION.
       01 WS-VARIABLES.
         05 WS-PGMNAME          PIC X(08) VALUE 'COSGN00C'.
         05 WS-TRANID           PIC X(04) VALUE 'CC00'.
         05 WS-USER-ID          PIC X(08).
         05 WS-USER-PWD         PIC X(08).
         05 WS-MESSAGE          PIC X(80).
         05 WS-RESP-CD          PIC S9(09) COMP VALUE ZEROS.

       COPY COSGN00.           *> BMS Map copybook
       COPY CSUSR01Y.          *> User security record

       PROCEDURE DIVISION.
       MAIN-PARA.
           IF EIBCALEN = 0
               *> First time - show signon screen
               MOVE LOW-VALUES TO COSGN0AO
               PERFORM SEND-SIGNON-SCREEN
           ELSE
               *> Process input
               EVALUATE EIBAID
                   WHEN DFHENTER
                       PERFORM PROCESS-ENTER-KEY
                   WHEN DFHPF3
                       MOVE 'Thank you' TO WS-MESSAGE
                       PERFORM SEND-PLAIN-TEXT
                   WHEN OTHER
                       MOVE 'Invalid key' TO WS-MESSAGE
                       PERFORM SEND-SIGNON-SCREEN
               END-EVALUATE
           END-IF.

           EXEC CICS RETURN
                TRANSID(WS-TRANID)
                COMMAREA(WS-COMMAREA)
                LENGTH(WS-COMM-LENGTH)
           END-EXEC.

       PROCESS-ENTER-KEY.
           *> Get input from map
           EXEC CICS RECEIVE MAP('COSGN0A')
                     MAPSET('COSGN00')
                     INTO(COSGN0AI)
           END-EXEC.

           *> Extract user input
           MOVE USERIDI OF COSGN0AI TO WS-USER-ID.
           MOVE PASSWORDI OF COSGN0AI TO WS-USER-PWD.

           *> Validate credentials
           PERFORM VALIDATE-USER.

           IF VALID-USER
               MOVE 'Login successful' TO WS-MESSAGE
               PERFORM TRANSFER-TO-MAIN-MENU
           ELSE
               MOVE 'Invalid credentials' TO WS-MESSAGE
               PERFORM SEND-SIGNON-SCREEN
           END-IF.

       VALIDATE-USER.
           *> Read user security file
           EXEC CICS READ FILE('USRSEC')
                     RIDFLD(WS-USER-ID)
                     INTO(USER-RECORD)
                     RESP(WS-RESP-CD)
           END-EXEC.

           IF WS-RESP-CD = DFHRESP(NORMAL)
               IF USER-PWD OF USER-RECORD = WS-USER-PWD
                   SET VALID-USER TO TRUE
               ELSE
                   SET INVALID-USER TO TRUE
               END-IF
           ELSE
               SET INVALID-USER TO TRUE
           END-IF.

       SEND-SIGNON-SCREEN.
           EXEC CICS SEND MAP('COSGN0A')
                     MAPSET('COSGN00')
                     FROM(COSGN0AO)
                     ERASE
           END-EXEC.
```

## Target: Java Spring Boot REST API

### 1. Entity (User Security)

```java
package com.carddemo.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_security")
public class UserSecurity {
    
    @Id
    @Column(length = 8)
    private String userId;
    
    @Column(length = 8, nullable = false)
    private String userPwd;
    
    @Column(length = 50)
    private String userName;
    
    @Column(length = 1)
    private String userType;  // 'A' = Admin, 'U' = User
    
    @Column(name = "last_signin")
    private LocalDateTime lastSignin;
    
    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    // Getters, setters, constructors
}
```

### 2. Repository

```java
package com.carddemo.repository;

import com.carddemo.model.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, String> {
    Optional<UserSecurity> findByUserId(String userId);
}
```

### 3. Service

```java
package com.carddemo.service;

import com.carddemo.model.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuthenticationService {
    
    @Autowired
    private UserSecurityRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtService jwtService;
    
    @Transactional
    public SigninResponse authenticateUser(String userId, String password) {
        // Validate inputs
        if (userId == null || userId.trim().isEmpty()) {
            throw new ValidationException("User ID is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }
        
        // Find user (equivalent to CICS READ)
        UserSecurity user = userRepository.findByUserId(userId.trim())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        
        // Check if account is locked
        if (user.getIsLocked()) {
            throw new AccountLockedException("Account is locked. Contact administrator.");
        }
        
        // Validate password (COBOL: IF USER-PWD = WS-USER-PWD)
        if (!passwordEncoder.matches(password, user.getUserPwd())) {
            handleFailedLogin(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }
        
        // Successful login
        handleSuccessfulLogin(user);
        
        // Generate JWT token
        String token = jwtService.generateToken(user);
        
        return new SigninResponse(
            "Login successful",
            token,
            user.getUserId(),
            user.getUserName(),
            user.getUserType()
        );
    }
    
    private void handleFailedLogin(UserSecurity user) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);
        if (user.getFailedAttempts() >= 3) {
            user.setIsLocked(true);
        }
        userRepository.save(user);
    }
    
    private void handleSuccessfulLogin(UserSecurity user) {
        user.setFailedAttempts(0);
        user.setLastSignin(LocalDateTime.now());
        userRepository.save(user);
    }
}
```

### 4. Controller

```java
package com.carddemo.controller;

import com.carddemo.service.AuthenticationService;
import com.carddemo.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * Authentication Controller
 * Equivalent to COBOL program COSGN00C (Transaction CC00)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * Signin endpoint
     * COBOL: EXEC CICS RECEIVE MAP + VALIDATE-USER paragraph
     */
    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(
            @RequestBody @Valid SigninRequest request) {
        
        SigninResponse response = authService.authenticateUser(
            request.userId(),
            request.password()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Signout endpoint
     * COBOL: EXEC CICS RETURN with no TRANSID
     */
    @PostMapping("/signout")
    public ResponseEntity<MessageResponse> signout() {
        // In stateless REST, signout is typically client-side
        // (delete JWT token from storage)
        return ResponseEntity.ok(new MessageResponse("Signout successful"));
    }
}
```

### 5. DTOs (Request/Response)

```java
package com.carddemo.dto;

import javax.validation.constraints.NotBlank;

// Request DTO (equivalent to input fields from BMS map)
public record SigninRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Password is required")
    String password
) {}

// Response DTO (equivalent to output fields to BMS map)
public record SigninResponse(
    String message,
    String token,
    String userId,
    String userName,
    String userType
) {}

public record MessageResponse(
    String message
) {}
```

### 6. Exception Handling

```java
package com.carddemo.controller;

import com.carddemo.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class AuthenticationExceptionHandler {
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

### 7. Testing

```java
package com.carddemo.controller;

import com.carddemo.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testSignin_ValidCredentials_Success() {
        // Given
        SigninRequest request = new SigninRequest("USER0001", "PASSWORD");
        
        // When
        ResponseEntity<SigninResponse> response = restTemplate
            .postForEntity("/api/auth/signin", request, SigninResponse.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Login successful", response.getBody().message());
        assertNotNull(response.getBody().token());
        assertEquals("USER0001", response.getBody().userId());
    }
    
    @Test
    void testSignin_InvalidCredentials_Unauthorized() {
        // Given
        SigninRequest request = new SigninRequest("USER0001", "WRONG");
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate
            .postForEntity("/api/auth/signin", request, ErrorResponse.class);
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().message());
    }
    
    @Test
    void testSignin_EmptyUserId_BadRequest() {
        // Given
        SigninRequest request = new SigninRequest("", "PASSWORD");
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate
            .postForEntity("/api/auth/signin", request, ErrorResponse.class);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
```

## Key Conversion Patterns Applied

1. **EXEC CICS RECEIVE MAP** → `@PostMapping` with `@RequestBody`
2. **EXEC CICS SEND MAP** → `ResponseEntity` with JSON
3. **EXEC CICS READ FILE** → JPA `findById()`
4. **EXEC CICS RETURN** → Method return
5. **CICS COMMAREA** → JWT token for stateless sessions
6. **PERFORM paragraphs** → Private service methods
7. **RESP code checking** → Exception handling
8. **BMS Map fields** → Request/Response DTOs

## Testing Equivalence

### COBOL Test Data
```
User ID:  USER0001
Password: PASSWORD
Expected: Login successful, transfer to main menu
```

### Java Test
```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"userId":"USER0001","password":"PASSWORD"}'

# Expected response:
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1...",
  "userId": "USER0001",
  "userName": "Test User",
  "userType": "U"
}
```

## Migration Checklist

- [x] CICS commands mapped to Spring Boot
- [x] BMS map converted to DTOs
- [x] COMMAREA replaced with JWT
- [x] File I/O converted to JPA
- [x] Error handling implemented
- [x] Unit tests created
- [x] Integration tests created
- [x] Security enhanced (password encoding, account locking)
- [x] Modern features added (JWT, REST, JSON)
