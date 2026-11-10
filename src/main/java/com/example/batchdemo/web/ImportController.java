package com.example.batchdemo.web;

import com.example.batchdemo.entity.Customer;
import com.example.batchdemo.repo.CustomerRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImportController {

    private final JobLauncher jobLauncher;
    private final Job importCustomersJob;
    private final JobExplorer jobExplorer;
    private final CustomerRepository customerRepository;

    public ImportController(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
                            Job importCustomersJob,
                            JobExplorer jobExplorer,
                            CustomerRepository customerRepository) {
        this.jobLauncher = jobLauncher;
        this.importCustomersJob = importCustomersJob;
        this.jobExplorer = jobExplorer;
        this.customerRepository = customerRepository;
    }

    /** Upload a CSV (header: name,email,city) and start the batch job. Returns 202 immediately. */
    @PostMapping("/customers/import")
    public ResponseEntity<Map<String, Object>> startImport(@RequestParam("file") MultipartFile file)
            throws Exception {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        Path tmp = Files.createTempFile("customers-import-", ".csv");
        file.transferTo(tmp);

        JobParameters params = new JobParametersBuilder()
                .addString("filePath", tmp.toString())
                .addLong("run.id", System.currentTimeMillis())   // makes every run a new JobInstance
                .toJobParameters();

        JobExecution execution = jobLauncher.run(importCustomersJob, params);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "jobExecutionId", execution.getId(),
                "status", execution.getStatus().toString(),
                "statusUrl", "/api/customers/import/" + execution.getId()));
    }

    /** Poll job progress. */
    @GetMapping("/customers/import/{id}")
    public Map<String, Object> importStatus(@PathVariable Long id) {
        JobExecution je = jobExplorer.getJobExecution(id);
        if (je == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No job execution " + id);
        }

        long read = 0, written = 0, filtered = 0, skipped = 0, commits = 0;
        for (StepExecution se : je.getStepExecutions()) {
            read += se.getReadCount();
            written += se.getWriteCount();
            filtered += se.getFilterCount();
            skipped += se.getSkipCount();
            commits += se.getCommitCount();
        }

        return Map.of(
                "jobExecutionId", je.getId(),
                "status", je.getStatus().toString(),
                "exitStatus", je.getExitStatus().getExitCode(),
                "readCount", read,
                "writeCount", written,
                "filterCount", filtered,
                "skipCount", skipped,
                "chunkCommits", commits);
    }

    @GetMapping("/customers")
    public List<Customer> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return customerRepository.findAll(PageRequest.of(page, size)).getContent();
    }

    @GetMapping("/customers/count")
    public Map<String, Long> count() {
        return Map.of("count", customerRepository.count());
    }
}
