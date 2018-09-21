package com.jojoldu.spring.springbatchinaction.persistwriter;

import com.jojoldu.spring.springbatchinaction.reader.jdbc.Pay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
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
public class JpaPersistWriterJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize = 10;

    @Bean
    public Job jpaPersistWriterJob() {
        return jobBuilderFactory.get("jpaPersistWriterJob")
                .start(jpaPersistWriterStep())
                .build();
    }

    @Bean
    public Step jpaPersistWriterStep() {
        return stepBuilderFactory.get("jpaPersistWriterStep")
                .<Pay, PayCopy>chunk(chunkSize)
                .reader(jpaPersistReader())
                .processor(jpaPersistProcessor())
                .writer(jpaPersistWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Pay> jpaPersistReader() {
        return new JpaPagingItemReaderBuilder<Pay>()
                .name("jpaPersistReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT p FROM Pay p")
                .build();
    }

    private ItemProcessor<Pay, PayCopy> jpaPersistProcessor() {
        return PayCopy::new;
    }

    private JpaItemPersistWriter<PayCopy> jpaPersistWriter() {
        return new JpaItemPersistWriter<>(PayCopy.class, entityManagerFactory);
    }

}
