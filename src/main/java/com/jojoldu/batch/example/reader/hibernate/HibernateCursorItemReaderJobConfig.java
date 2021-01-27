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
import org.springframework.batch.item.database.HibernateCursorItemReader;
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class HibernateCursorItemReaderJobConfig {
    public static final String JOB_NAME = "hibernateCursorItemReaderJob";
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize;

    @Value("${chunkSize:5}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean
    public Job hibernateCursorItemReaderJob() {
        return jobBuilderFactory.get("hibernateCursorItemReaderJob")
                .start(hibernateCursorItemReaderStep())
                .build();
    }

    @Bean
    public Step hibernateCursorItemReaderStep() {
        return stepBuilderFactory.get("hibernateCursorItemReaderStep")
                .<Pay, Pay>chunk(chunkSize)
                .reader(hibernateCursorItemReader())
                .writer(hibernateCursorItemWriter())
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public HibernateCursorItemReader<Pay> hibernateCursorItemReader() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

        return new HibernateCursorItemReaderBuilder<Pay>()
                .sessionFactory(sessionFactory)
                .queryString("SELECT p FROM Pay p")
                .name(JOB_NAME +"_reader")
                .useStatelessSession(false)
                .fetchSize(1)
                .maxItemCount(5)
                .build();
    }

    private ItemWriter<Pay> hibernateCursorItemWriter() {
        return list -> {
            for (Pay pay: list) {
                log.info("Current Pay={}", pay);
            }
        };
    }
}
