package com.jojoldu.batch.example.performancewrite.test4;

import com.jojoldu.batch.entity.product.Store;
import com.jojoldu.batch.entity.product.backup.StoreBackup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.HibernateCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;


/**
 * Created by jojoldu@gmail.com on 06/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class StoreBackup4Configuration {
    public static final String JOB_NAME = "storeBackupJob4";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;

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
    public HibernateCursorItemReader<Store> reader(@Value("#{jobParameters[storeName]}") String storeName) {
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        String query = String.format(
                "SELECT s " +
                "FROM Store s " +
                "WHERE s.name ='%s'", storeName);

        return new HibernateCursorItemReaderBuilder<Store>()
                .sessionFactory(sessionFactory)
                .queryString(query)
                .name("reader")
                .useStatelessSession(false)
                .build();
    }

    private ItemProcessor<Store, StoreBackup> processor() {
        return StoreBackup::new;
    }

    @Bean(name = JOB_NAME +"_writer")
    public JpaItemWriter<StoreBackup> writer() {
        return new JpaItemWriterBuilder<StoreBackup>()
                .entityManagerFactory(emf)
                .build();
    }
}
