package com.jojoldu.batch.example.multithread;

import com.jojoldu.batch.entity.product.Product;
import com.jojoldu.batch.entity.product.backup.ProductBackup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MultiThreadCursorConfiguration {
    public static final String JOB_NAME = "multiThreadCursorBatch";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    private int chunkSize;

    @Value("${chunkSize:1000}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    private int poolSize;

    @Value("${poolSize:10}")
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    @Bean(name = JOB_NAME+"taskPool")
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("multi-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.initialize();
        return executor;
    }

    @Bean(name = JOB_NAME)
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step())
                .preventRestart()
                .build();
    }

    @Bean(name = JOB_NAME +"_step")
    @JobScope
    public Step step() {
        return stepBuilderFactory.get(JOB_NAME +"_step")
                .<Product, ProductBackup>chunk(chunkSize)
                .reader(reader(null))
                .listener(new CursorItemReaderListener()) // (1)
                .processor(processor())
                .writer(writer())
                .taskExecutor(executor())
                .throttleLimit(poolSize)
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public SynchronizedItemStreamReader<Product> reader(@Value("#{jobParameters[createDate]}") String createDate) {
        String sql = "SELECT id, name, price, create_date, status FROM product WHERE create_date=':createDate'"
                .replace(":createDate", createDate);

        JdbcCursorItemReader<Product> itemReader = new JdbcCursorItemReaderBuilder<Product>()
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(Product.class))
                .sql(sql)
                .name(JOB_NAME + "_reader")
                .build();

        return new SynchronizedItemStreamReaderBuilder<Product>()
                .delegate(itemReader)
                .build();
    }

//    @Bean(name = JOB_NAME +"_reader")
//    @StepScope
//    public JdbcCursorItemReader<Product> reader(@Value("#{jobParameters[createDate]}") String createDate) {
//        String sql = "SELECT id, name, price, create_date, status FROM product WHERE create_date=':createDate'"
//                .replace(":createDate", createDate);
//
//        return new JdbcCursorItemReaderBuilder<Product>()
//                .fetchSize(chunkSize)
//                .dataSource(dataSource)
//                .rowMapper(new BeanPropertyRowMapper<>(Product.class))
//                .sql(sql)
//                .name(JOB_NAME + "_reader")
//                .build();
//    }

    private ItemProcessor<Product, ProductBackup> processor() {
        return item -> {
            log.info("Processing Start Item id={}", item.getId());
            Thread.sleep(1000);
            log.info("Processing End Item id={}", item.getId());
            return new ProductBackup(item);
        };
    }

    @Bean(name = JOB_NAME +"_writer")
    @StepScope
    public JpaItemWriter<ProductBackup> writer() {
        return new JpaItemWriterBuilder<ProductBackup>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
