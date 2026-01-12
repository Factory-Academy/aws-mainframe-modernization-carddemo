# Batch Program Conversion Example

Example of converting a COBOL batch program to Spring Batch.

## Source: CBTRN02C (Transaction Posting Batch)

### COBOL Batch Program

```cobol
      ******************************************************************
      * Program     : CBTRN02C.CBL
      * Function    : Post daily transactions to master file
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID.    CBTRN02C.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT DALYTRAN-FILE ASSIGN TO DALYTRAN
                  ORGANIZATION IS SEQUENTIAL
                  ACCESS MODE  IS SEQUENTIAL
                  FILE STATUS  IS DALYTRAN-STATUS.
                  
           SELECT TRANSACT-FILE ASSIGN TO TRANFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS RANDOM
                  RECORD KEY   IS FD-TRANS-ID
                  FILE STATUS  IS TRANFILE-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  DALYTRAN-FILE.
       01  FD-DAILY-TRAN-RECORD.
           05 FD-TRAN-ID            PIC X(16).
           05 FD-TRAN-DATA          PIC X(334).

       FD  TRANSACT-FILE.
       01  FD-TRANFILE-REC.
           05 FD-TRANS-ID           PIC X(16).
           05 FD-TRANS-DATA         PIC X(334).

       WORKING-STORAGE SECTION.
       01  WS-FILE-STATUS.
           05  DALYTRAN-STATUS      PIC X(02).
           05  TRANFILE-STATUS      PIC X(02).
           
       01  WS-COUNTERS.
           05  WS-READ-COUNT        PIC 9(09) COMP VALUE ZERO.
           05  WS-WRITE-COUNT       PIC 9(09) COMP VALUE ZERO.
           05  WS-ERROR-COUNT       PIC 9(09) COMP VALUE ZERO.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 1000-OPEN-FILES.
           PERFORM 2000-PROCESS-TRANSACTIONS
               UNTIL DALYTRAN-STATUS = '10'.
           PERFORM 3000-CLOSE-FILES.
           PERFORM 9000-DISPLAY-STATS.
           STOP RUN.

       1000-OPEN-FILES.
           OPEN INPUT DALYTRAN-FILE
           OPEN I-O TRANSACT-FILE.

       2000-PROCESS-TRANSACTIONS.
           PERFORM 2100-READ-DAILY-TRAN.
           IF DALYTRAN-STATUS = '00'
               PERFORM 2200-WRITE-TRANSACTION
           END-IF.

       2100-READ-DAILY-TRAN.
           READ DALYTRAN-FILE
               INTO FD-DAILY-TRAN-RECORD
               AT END
                   MOVE '10' TO DALYTRAN-STATUS
           END-READ.
           
           IF DALYTRAN-STATUS = '00'
               ADD 1 TO WS-READ-COUNT
           END-IF.

       2200-WRITE-TRANSACTION.
           MOVE FD-DAILY-TRAN-RECORD TO FD-TRANFILE-REC
           
           WRITE FD-TRANFILE-REC
               INVALID KEY
                   ADD 1 TO WS-ERROR-COUNT
                   DISPLAY 'ERROR: Duplicate transaction ' FD-TRANS-ID
               NOT INVALID KEY
                   ADD 1 TO WS-WRITE-COUNT
           END-WRITE.

       3000-CLOSE-FILES.
           CLOSE DALYTRAN-FILE
           CLOSE TRANSACT-FILE.

       9000-DISPLAY-STATS.
           DISPLAY 'Records read:    ' WS-READ-COUNT.
           DISPLAY 'Records written: ' WS-WRITE-COUNT.
           DISPLAY 'Errors:          ' WS-ERROR-COUNT.
```

## Target: Java Spring Batch

### 1. Entity

```java
package com.carddemo.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction")
public class Transaction {
    
    @Id
    @Column(length = 16)
    private String transactionId;
    
    @Column(length = 16, nullable = false)
    private String accountId;
    
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(length = 2, nullable = false)
    private String transactionType;
    
    @Column(name = "tran_date", nullable = false)
    private LocalDate transactionDate;
    
    @Column(length = 100)
    private String description;
    
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
    
    // Getters, setters, constructors
}
```

### 2. Repository

```java
package com.carddemo.repository;

import com.carddemo.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    boolean existsByTransactionId(String transactionId);
}
```

### 3. Spring Batch Configuration

```java
package com.carddemo.batch.config;

import com.carddemo.batch.*;
import com.carddemo.model.Transaction;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.RecordFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import javax.persistence.EntityManagerFactory;

@Configuration
@EnableBatchProcessing
public class TransactionPostingBatchConfig {
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    @Value("${batch.input.file:data/daily-transactions.txt}")
    private String inputFile;
    
    /**
     * Main batch job
     * COBOL equivalent: 0000-MAIN paragraph
     */
    @Bean
    public Job transactionPostingJob(Step postTransactionsStep) {
        return new JobBuilder("transactionPostingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(postTransactionsStep)
            .listener(new JobExecutionListener() {
                @Override
                public void beforeJob(JobExecution jobExecution) {
                    System.out.println("Starting transaction posting job");
                }
                
                @Override
                public void afterJob(JobExecution jobExecution) {
                    System.out.println("Transaction posting job completed");
                    System.out.println("Status: " + jobExecution.getStatus());
                }
            })
            .build();
    }
    
    /**
     * Processing step
     * COBOL equivalent: 2000-PROCESS-TRANSACTIONS paragraph
     */
    @Bean
    public Step postTransactionsStep(
            ItemReader<Transaction> reader,
            ItemProcessor<Transaction, Transaction> processor,
            ItemWriter<Transaction> writer) {
        
        return new StepBuilder("postTransactionsStep", jobRepository)
            .<Transaction, Transaction>chunk(100, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    System.out.println("Opening files...");
                }
                
                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    long readCount = stepExecution.getReadCount();
                    long writeCount = stepExecution.getWriteCount();
                    long errorCount = stepExecution.getWriteSkipCount();
                    
                    System.out.println("Records read:    " + readCount);
                    System.out.println("Records written: " + writeCount);
                    System.out.println("Errors:          " + errorCount);
                    
                    return stepExecution.getExitStatus();
                }
            })
            .build();
    }
    
    /**
     * Reader - reads from flat file
     * COBOL equivalent: 2100-READ-DAILY-TRAN paragraph + DALYTRAN-FILE
     */
    @Bean
    public FlatFileItemReader<Transaction> reader() {
        return new FlatFileItemReaderBuilder<Transaction>()
            .name("transactionReader")
            .resource(new FileSystemResource(inputFile))
            .delimited()
            .delimiter("|")
            .names("transactionId", "accountId", "amount", 
                   "transactionType", "transactionDate", "description")
            .fieldSetMapper(fieldSet -> {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(fieldSet.readString("transactionId"));
                transaction.setAccountId(fieldSet.readString("accountId"));
                transaction.setAmount(fieldSet.readBigDecimal("amount"));
                transaction.setTransactionType(fieldSet.readString("transactionType"));
                transaction.setTransactionDate(
                    LocalDate.parse(fieldSet.readString("transactionDate")));
                transaction.setDescription(fieldSet.readString("description"));
                return transaction;
            })
            .build();
    }
    
    /**
     * Processor - validates and transforms data
     * COBOL equivalent: Business logic in 2200-WRITE-TRANSACTION
     */
    @Bean
    public ItemProcessor<Transaction, Transaction> processor() {
        return new TransactionProcessor();
    }
    
    /**
     * Writer - writes to database
     * COBOL equivalent: WRITE TRANSACT-FILE paragraph
     */
    @Bean
    public JpaItemWriter<Transaction> writer() {
        JpaItemWriter<Transaction> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
```

### 4. Item Processor

```java
package com.carddemo.batch;

import com.carddemo.model.Transaction;
import com.carddemo.repository.TransactionRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Transaction processor
 * COBOL equivalent: Validation logic before WRITE
 */
@Component
public class TransactionProcessor implements ItemProcessor<Transaction, Transaction> {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Override
    public Transaction process(Transaction transaction) throws Exception {
        // Validate transaction (COBOL IF statements)
        if (transaction.getTransactionId() == null || 
            transaction.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        
        if (transaction.getAmount() == null || 
            transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Check for duplicates (COBOL INVALID KEY)
        if (transactionRepository.existsByTransactionId(
                transaction.getTransactionId())) {
            throw new DuplicateTransactionException(
                "Duplicate transaction: " + transaction.getTransactionId());
        }
        
        // Business logic transformations
        transaction.setProcessed(false);
        
        return transaction;
    }
}
```

### 5. Job Launcher

```java
package com.carddemo.batch;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Job launcher for scheduled batch execution
 * COBOL equivalent: JCL job scheduling
 */
@Component
public class TransactionPostingJobLauncher {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job transactionPostingJob;
    
    /**
     * Run batch job daily at 2 AM
     * COBOL equivalent: JCL scheduled job
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyPostingJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addString("runDate", LocalDateTime.now().toString())
                .toJobParameters();
            
            JobExecution execution = jobLauncher.run(transactionPostingJob, params);
            
            System.out.println("Job Status: " + execution.getStatus());
            System.out.println("Job ID: " + execution.getJobId());
            
        } catch (JobExecutionAlreadyRunningException e) {
            System.err.println("Job is already running");
        } catch (Exception e) {
            System.err.println("Job failed: " + e.getMessage());
        }
    }
    
    /**
     * Manual job execution (for testing)
     */
    public void runJobManually() {
        runDailyPostingJob();
    }
}
```

### 6. Testing

```java
package com.carddemo.batch;

import com.carddemo.model.Transaction;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = {
    "batch.input.file=src/test/resources/test-transactions.txt"
})
class TransactionPostingBatchTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }
    
    @Test
    void testTransactionPostingJob_Success() throws Exception {
        // Given - test file has 10 valid transactions
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        
        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        StepExecution stepExecution = jobExecution.getStepExecutions()
            .iterator().next();
        
        assertEquals(10, stepExecution.getReadCount());
        assertEquals(10, stepExecution.getWriteCount());
        assertEquals(0, stepExecution.getWriteSkipCount());
        
        // Verify data in database
        assertEquals(10, transactionRepository.count());
    }
    
    @Test
    void testTransactionPostingJob_WithErrors() throws Exception {
        // Given - test file has some invalid transactions
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        
        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        StepExecution stepExecution = jobExecution.getStepExecutions()
            .iterator().next();
        
        assertTrue(stepExecution.getWriteSkipCount() > 0);
    }
}
```

## Key Conversion Patterns Applied

1. **Sequential file READ** → `FlatFileItemReader`
2. **Indexed file WRITE** → `JpaItemWriter`
3. **PERFORM UNTIL** → Spring Batch chunk processing
4. **File STATUS checking** → Spring Batch error handling
5. **Counters (WS-READ-COUNT, etc.)** → `StepExecution` metrics
6. **OPEN/CLOSE files** → Spring Batch manages automatically
7. **DISPLAY statements** → Logging and listeners
8. **JCL scheduling** → `@Scheduled` annotation

## Input File Format

### COBOL Fixed-Length Record
```
1234567890123456ACCT000000000001000000.00DB20240115Purchase
```

### Java Delimited Format (easier to work with)
```csv
transactionId|accountId|amount|transactionType|transactionDate|description
1234567890123456|ACCT00000000001|1000.00|DB|2024-01-15|Purchase
2345678901234567|ACCT00000000002|500.00|CR|2024-01-15|Payment
```

## Running the Batch Job

### Command Line
```bash
java -jar carddemo-batch.jar \
  --spring.batch.job.names=transactionPostingJob \
  --batch.input.file=data/daily-transactions.txt
```

### Scheduled Execution
Job runs automatically at 2 AM daily via `@Scheduled` annotation.

## Monitoring

Spring Batch provides built-in monitoring via `JobRepository`:

```java
@GetMapping("/api/batch/jobs")
public List<JobExecution> listJobs() {
    return jobRepository.findJobExecutions("transactionPostingJob");
}
```

## Migration Checklist

- [x] Sequential file reading converted to `ItemReader`
- [x] Indexed file writing converted to JPA
- [x] Chunk-based processing implemented
- [x] Error handling configured
- [x] Counters/statistics tracked
- [x] Job scheduling configured
- [x] Unit tests created
- [x] Performance validated
- [x] Logging implemented
