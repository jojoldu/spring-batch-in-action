package com.jojoldu.batch.example.reader.hibernate;

import com.jojoldu.batch.entity.pay.Pay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.HibernatePagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.HibernatePagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class HibernatePagingItemReaderJobConfig {
    public static final String JOB_NAME = "hibernatePagingItemReaderJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize;

    @Value("${chunkSize:5}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean
    public Job hibernatePagingItemReaderJob() {
        return jobBuilderFactory.get("hibernatePagingItemReaderJob")
                .start(hibernatePagingItemReaderStep())
                .build();
    }

    @Bean
    public Step hibernatePagingItemReaderStep() {
        return stepBuilderFactory.get("hibernatePagingItemReaderStep")
                .<Pay, Pay>chunk(chunkSize)
                .reader(hibernatePagingItemReader())
                .writer(hibernatePagingItemWriter())
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public HibernatePagingItemReader<Pay> hibernatePagingItemReader() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

        return new HibernatePagingItemReaderBuilder<Pay>()
                .sessionFactory(sessionFactory)
                .queryString("SELECT p FROM Pay p")
                .name(JOB_NAME +"_reader")
                .useStatelessSession(false)
                .fetchSize(chunkSize)
                .pageSize(chunkSize)
                .build();
    }

    private ItemWriter<Pay> hibernatePagingItemWriter() {
        return list -> {
            for (Pay pay: list) {
                log.info("Current Pay={}", pay);
            }
        };
    }

}
