package com.jojoldu.batch.example.performancewrite.test3;

import com.jojoldu.batch.entity.product.Store;
import com.jojoldu.batch.entity.product.backup.StoreBackup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;


/**
 * Created by jojoldu@gmail.com on 06/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class StoreBackup3Configuration {
    public static final String JOB_NAME = "storeBackupJob3";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;
    private final DataSource dataSource;
    private final BulkInsertRepository bulkInsertRepository;

    private int chunkSize;

    @Value("${chunkSize:1000}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean(name = JOB_NAME)
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step())
                .build();
    }

    @Bean(name = JOB_NAME +"_step")
    @JobScope
    public Step step() {
        return stepBuilderFactory.get("step")
                .<Store, StoreBackup>chunk(chunkSize)
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public JpaPagingItemReader<Store> reader(@Value("#{jobParameters[storeName]}") String storeName) {
        String query = String.format(
                "SELECT s " +
                "FROM Store s " +
                "WHERE s.name ='%s'", storeName);

        return new JpaPagingItemReaderBuilder<Store>()
                .entityManagerFactory(emf)
                .queryString(query)
                .pageSize(chunkSize)
                .name("reader")
                .build();
    }

    private ItemProcessor<Store, StoreBackup> processor() {
        return StoreBackup::new;
    }

    @Bean(name = JOB_NAME +"_writer")
    @StepScope
    public JdbcBatchItemWriter<StoreBackup> writer() {
        return new JdbcBatchItemWriterBuilder<StoreBackup>()
                .dataSource(dataSource)
                .sql("insert into store_backup(origin_id, name) values (:originId, :name)")
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

//    @Bean
//    @StepScope
//    public ItemWriter<StoreBackup> writer () {
//        return items -> bulkInsertRepository.saveAll(new ArrayList<>(items));
//    }
}
