package com.carddemo.controller;

import com.carddemo.model.*;
import com.carddemo.service.*;
import com.carddemo.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

/**
 * REST Controller template for CICS program conversion
 * 
 * This template provides structure for converting CICS COBOL programs
 * to Spring Boot REST controllers.
 * 
 * Mapping:
 * - EXEC CICS RECEIVE MAP → @PostMapping with @RequestBody
 * - EXEC CICS SEND MAP → ResponseEntity with JSON
 * - Transaction ID (e.g., CC00) → Endpoint path
 * - COMMAREA → Request/Response DTOs
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    /**
     * Get account details
     * COBOL Transaction: CAVW (Account View)
     * CICS: EXEC CICS READ FILE('ACCTDAT') ...
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountId) {
        
        Account account = accountService.getAccount(accountId);
        
        AccountResponse response = new AccountResponse(
            account.getAccountId(),
            account.getAccountName(),
            account.getBalance(),
            account.getStatus()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create new account
     * COBOL Transaction: CADD (Account Add)
     * CICS: EXEC CICS WRITE FILE('ACCTDAT') ...
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestBody @Valid AccountCreateRequest request) {
        
        Account account = new Account();
        account.setAccountId(request.accountId());
        account.setAccountName(request.accountName());
        account.setCreditLimit(request.creditLimit());
        
        Account created = accountService.createAccount(account);
        
        AccountResponse response = new AccountResponse(
            created.getAccountId(),
            created.getAccountName(),
            created.getBalance(),
            created.getStatus()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Update account
     * COBOL Transaction: CAUP (Account Update)
     * CICS: EXEC CICS READ UPDATE ... EXEC CICS REWRITE ...
     */
    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String accountId,
            @RequestBody @Valid AccountUpdateRequest request) {
        
        Account account = accountService.getAccount(accountId);
        
        // Update fields
        if (request.accountName() != null) {
            account.setAccountName(request.accountName());
        }
        if (request.creditLimit() != null) {
            account.setCreditLimit(request.creditLimit());
        }
        
        Account updated = accountService.updateAccount(account);
        
        AccountResponse response = new AccountResponse(
            updated.getAccountId(),
            updated.getAccountName(),
            updated.getBalance(),
            updated.getStatus()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * List accounts
     * COBOL Transaction: CALI (Account List)
     * CICS: EXEC CICS STARTBR ... READNEXT loop
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @RequestParam(required = false) String status) {
        
        List<Account> accounts;
        if (status != null) {
            accounts = accountService.findByStatus(status);
        } else {
            accounts = accountService.findAll();
        }
        
        List<AccountResponse> responses = accounts.stream()
            .map(acct -> new AccountResponse(
                acct.getAccountId(),
                acct.getAccountName(),
                acct.getBalance(),
                acct.getStatus()
            ))
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Process transaction on account
     * COBOL Transaction: CTPR (Transaction Process)
     * CICS: Multiple file operations in single transaction
     */
    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> processTransaction(
            @PathVariable String accountId,
            @RequestBody @Valid TransactionRequest request) {
        
        Transaction transaction = accountService.processTransaction(
            accountId, 
            request
        );
        
        TransactionResponse response = new TransactionResponse(
            transaction.getTransactionId(),
            transaction.getAccountId(),
            transaction.getAmount(),
            transaction.getTransactionType(),
            transaction.getTransactionDate()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Close account
     * COBOL Transaction: CACL (Account Close)
     */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> closeAccount(
            @PathVariable String accountId,
            @RequestParam(required = false) String reason) {
        
        accountService.closeAccount(accountId, reason);
        
        return ResponseEntity.noContent().build();
    }
}

/**
 * Global exception handler
 * Converts service exceptions to appropriate HTTP responses
 * COBOL equivalent: RESP code checking after EXEC CICS commands
 */
@RestControllerAdvice
class AccountExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateAccountException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Internal server error"));
    }
}
