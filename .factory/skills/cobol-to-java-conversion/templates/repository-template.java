package com.carddemo.repository;

import com.carddemo.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository template for VSAM file access conversion
 * 
 * Spring Data JPA provides automatic implementation of common database operations.
 * 
 * VSAM File Operations → JPA Repository Methods:
 * - READ → findById()
 * - WRITE → save() with new entity
 * - REWRITE → save() with existing entity
 * - DELETE → deleteById()
 * - STARTBR/READNEXT → findBy...() methods or custom queries
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    
    /**
     * Find by status
     * COBOL: STARTBR with generic key, READNEXT filtering by status
     */
    List<Account> findByStatus(String status);
    
    /**
     * Find by status and balance greater than
     * COBOL: STARTBR/READNEXT loop with IF condition
     */
    List<Account> findByStatusAndBalanceGreaterThan(
        String status, 
        BigDecimal balance
    );
    
    /**
     * Find by account ID starting with prefix
     * COBOL: STARTBR with partial key, READNEXT until key changes
     */
    List<Account> findByAccountIdStartingWith(String prefix);
    
    /**
     * Find accounts opened within date range
     * COBOL: STARTBR/READNEXT loop with date checking
     */
    List<Account> findByOpenDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Check if account exists
     * COBOL: EXEC CICS READ with NOTFND handling
     */
    boolean existsByAccountId(String accountId);
    
    /**
     * Count accounts by status
     * COBOL: STARTBR/READNEXT loop with counter
     */
    long countByStatus(String status);
    
    /**
     * Custom query example
     * COBOL: Complex STARTBR/READNEXT logic
     */
    @Query("SELECT a FROM Account a WHERE a.status = :status " +
           "AND a.balance > :minBalance " +
           "ORDER BY a.balance DESC")
    List<Account> findHighBalanceAccounts(
        @Param("status") String status,
        @Param("minBalance") BigDecimal minBalance
    );
    
    /**
     * Native SQL query example (for complex queries)
     * Use when JPA query is too complex
     */
    @Query(value = "SELECT * FROM account " +
                   "WHERE status = ?1 " +
                   "AND balance BETWEEN ?2 AND ?3 " +
                   "ORDER BY open_date DESC " +
                   "LIMIT ?4",
           nativeQuery = true)
    List<Account> findAccountsInBalanceRange(
        String status,
        BigDecimal minBalance,
        BigDecimal maxBalance,
        int limit
    );
    
    /**
     * Update query example
     * COBOL: READ UPDATE ... REWRITE loop
     */
    @Query("UPDATE Account a SET a.status = :newStatus " +
           "WHERE a.status = :oldStatus")
    int updateStatusForAll(
        @Param("oldStatus") String oldStatus,
        @Param("newStatus") String newStatus
    );
}

/**
 * Example of repository for cross-reference table
 * COBOL: Secondary VSAM file with alternate index
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {
    
    /**
     * Find by customer ID
     * COBOL: AIX (Alternate Index) lookup
     */
    Optional<CardXref> findByCustId(Long custId);
    
    /**
     * Find by account ID
     * COBOL: Another AIX lookup
     */
    List<CardXref> findByAcctId(Long acctId);
    
    /**
     * Check if card number exists
     * COBOL: READ with NOTFND check
     */
    boolean existsByCardNum(String cardNum);
}

/**
 * Example of custom repository implementation
 * Use when Spring Data JPA methods are insufficient
 */
@Repository
public class CustomAccountRepositoryImpl {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Complex query that's hard to express with JPA
     * COBOL equivalent: Complex STARTBR/READNEXT logic
     */
    public List<Account> findAccountsWithComplexCriteria(
            String status,
            BigDecimal minBalance,
            LocalDate startDate) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Account> query = cb.createQuery(Account.class);
        Root<Account> account = query.from(Account.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (status != null) {
            predicates.add(cb.equal(account.get("status"), status));
        }
        
        if (minBalance != null) {
            predicates.add(cb.greaterThan(
                account.get("balance"), minBalance));
        }
        
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                account.get("openDate"), startDate));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(account.get("balance")));
        
        return entityManager.createQuery(query).getResultList();
    }
}
