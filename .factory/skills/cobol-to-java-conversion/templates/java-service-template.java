package com.carddemo.service;

import com.carddemo.model.*;
import com.carddemo.repository.*;
import com.carddemo.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service class template for COBOL business logic conversion
 * 
 * This template provides a structure for converting COBOL PROCEDURE DIVISION
 * logic to Java service methods.
 * 
 * Key principles:
 * - Each COBOL paragraph/section becomes a method
 * - Use @Transactional for SYNCPOINT equivalent
 * - Throw exceptions instead of COBOL error flags
 * - Use BigDecimal for all financial calculations
 */
@Service
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Example: Get account by ID
     * COBOL equivalent: EXEC CICS READ FILE('ACCTDAT') ...
     */
    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(
                "Account not found: " + accountId));
    }
    
    /**
     * Example: Create new account
     * COBOL equivalent: EXEC CICS WRITE FILE('ACCTDAT') ...
     */
    @Transactional
    public Account createAccount(Account account) {
        // Validation (equivalent to COBOL IF statements)
        if (accountRepository.existsById(account.getAccountId())) {
            throw new DuplicateAccountException(
                "Account already exists: " + account.getAccountId());
        }
        
        // Business logic
        account.setBalance(BigDecimal.ZERO);
        account.setOpenDate(LocalDate.now());
        account.setStatus("A");
        
        return accountRepository.save(account);
    }
    
    /**
     * Example: Update account balance
     * COBOL equivalent: 
     *   EXEC CICS READ UPDATE FILE('ACCTDAT') ...
     *   ADD AMOUNT TO BALANCE.
     *   EXEC CICS REWRITE FILE('ACCTDAT') ...
     *   EXEC CICS SYNCPOINT.
     */
    @Transactional
    public Account updateBalance(String accountId, BigDecimal amount) {
        Account account = getAccount(accountId);
        
        // Business logic (equivalent to COBOL PERFORM paragraphs)
        validateAmount(amount);
        checkCreditLimit(account, amount);
        
        // Update balance (COMP-3 → BigDecimal preserves precision)
        account.setBalance(account.getBalance().add(amount));
        
        // Save (equivalent to REWRITE)
        return accountRepository.save(account);
        
        // @Transactional handles SYNCPOINT automatically
    }
    
    /**
     * Example: Process transaction
     * COBOL equivalent: PERFORM PROCESS-TRANSACTION paragraph
     */
    @Transactional
    public Transaction processTransaction(TransactionRequest request) {
        // Validate inputs
        validateTransactionRequest(request);
        
        // Get account
        Account account = getAccount(request.getAccountId());
        
        // Check business rules
        if (!"A".equals(account.getStatus())) {
            throw new AccountInactiveException("Account is not active");
        }
        
        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getType());
        transaction.setTransactionDate(LocalDate.now());
        
        // Update account balance
        updateBalance(request.getAccountId(), request.getAmount());
        
        // Save transaction
        return transactionRepository.save(transaction);
    }
    
    /**
     * Private helper method (equivalent to COBOL paragraph)
     * COBOL equivalent: VALIDATE-AMOUNT section
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
    }
    
    /**
     * Private helper method
     * COBOL equivalent: CHECK-CREDIT-LIMIT section
     */
    private void checkCreditLimit(Account account, BigDecimal amount) {
        BigDecimal newBalance = account.getBalance().add(amount);
        if (newBalance.compareTo(account.getCreditLimit()) > 0) {
            throw new CreditLimitExceededException(
                "Transaction would exceed credit limit");
        }
    }
    
    /**
     * Private helper method
     * COBOL equivalent: VALIDATE-TRAN-REQUEST section
     */
    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getAccountId() == null || request.getAccountId().isBlank()) {
            throw new ValidationException("Account ID is required");
        }
        validateAmount(request.getAmount());
    }
    
    /**
     * Example: List accounts with criteria
     * COBOL equivalent: STARTBR/READNEXT loop
     */
    public List<Account> findActiveAccounts() {
        return accountRepository.findByStatus("A");
    }
    
    /**
     * Example: Close account (soft delete)
     * COBOL equivalent: REWRITE with status change
     */
    @Transactional
    public Account closeAccount(String accountId, String reason) {
        Account account = getAccount(accountId);
        
        // Check if account can be closed
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountNotClosableException(
                "Account has non-zero balance");
        }
        
        account.setStatus("C");  // Closed
        account.setCloseDate(LocalDate.now());
        account.setCloseReason(reason);
        
        return accountRepository.save(account);
    }
}
