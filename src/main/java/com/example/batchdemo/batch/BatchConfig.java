package com.example.batchdemo.batch;

import com.example.batchdemo.entity.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    /** Commit every 50 items. Keep equal to hibernate.jdbc.batch_size. */
    public static final int CHUNK_SIZE = 50;

    /** Async launcher so the REST call returns immediately instead of blocking until the job ends. */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("import-job-"));
        launcher.afterPropertiesSet();
        return launcher;
    }

    /** Reader is @StepScope so it can read the uploaded file's path from the job parameters. */
    @Bean
    @StepScope
    public FlatFileItemReader<CustomerCsv> customerReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<CustomerCsv>()
                .name("customerCsvReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)                       // header row
                .delimited()
                .names("name", "email", "city")
                .fieldSetMapper(fs -> new CustomerCsv(
                        fs.readString("name"),
                        fs.readString("email"),
                        fs.readString("city")))
                .build();
    }

    @Bean
    public Step importCustomersStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    FlatFileItemReader<CustomerCsv> customerReader,
                                    CustomerProcessor processor,
                                    CustomerUpsertWriter writer) {
        return new StepBuilder("importCustomersStep", jobRepository)
                .<CustomerCsv, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(FlatFileParseException.class)   // malformed CSV lines are skipped, not fatal
                .skipLimit(100)
                .build();
    }

    @Bean
    public Job importCustomersJob(JobRepository jobRepository, Step importCustomersStep) {
        return new JobBuilder("importCustomersJob", jobRepository)
                .start(importCustomersStep)
                .build();
    }
}
