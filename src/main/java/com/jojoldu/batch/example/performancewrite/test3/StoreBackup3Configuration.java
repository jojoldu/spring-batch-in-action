package com.jojoldu.batch.example.performancewrite.test3;

import com.jojoldu.batch.entity.product.Store;
import com.jojoldu.batch.entity.product.backup.StoreBackup;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.BeanMapper;
import com.querydsl.sql.dml.SQLInsertClause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

import static com.jojoldu.batch.entity.product.backup.QStoreBackup.storeBackup;


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
    private final SQLQueryFactory sqlQueryFactory;

    private int chunkSize;

    @Value("${chunkSize:1000}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step())
                .build();
    }

    @Bean
    @JobScope
    public Step step() {
        return stepBuilderFactory.get("step")
                .<Store, StoreBackup>chunk(chunkSize)
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Store> reader(@Value("#{jobParameters[storeName]}") String storeName) {
        String query = String.format(
                "SELECT s " +
                "FROM Store s " +
                "JOIN FETCH s.products " +
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

    @Bean
    public ItemWriter<StoreBackup> writer() {
        return items -> {
            RelationalPathBase<StoreBackup> qStoreBackup = new RelationalPathBase<>(
                    storeBackup.getType(),
                    storeBackup.getMetadata(),
                    "jojoldu",
                    "store_backup");

            SQLInsertClause insert = sqlQueryFactory.insert(qStoreBackup);

            for (StoreBackup item : items) {
                insert.populate(item, BeanMapper.WITH_NULL_BINDINGS).addBatch();
            }

            long execute = insert.execute();
            log.info("등록 건수={}", execute);
        };
    }
}
