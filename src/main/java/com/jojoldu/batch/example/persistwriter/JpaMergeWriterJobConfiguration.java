package com.jojoldu.batch.example.persistwriter;

import com.jojoldu.batch.entity.pay.Pay;
import com.jojoldu.batch.entity.pay.PayCopy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

/**
 * Created by jojoldu@gmail.com on 26/09/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class JpaMergeWriterJobConfiguration {
    public static final String JOB_NAME = "jpaMergeWriterJob";
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize = 10;

    @Bean
    public Job jpaMergeWriterJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(jpaMergeWriterStep())
                .build();
    }

    @Bean
    public Step jpaMergeWriterStep() {
        return stepBuilderFactory.get(JOB_NAME+"Step")
                .<Pay, PayCopy>chunk(chunkSize)
                .reader(jpaMergeReader())
                .processor(jpaMergeProcessor())
                .writer(jpaMergeWriter())
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    public JpaPagingItemReader<Pay> jpaMergeReader() {
        return new JpaPagingItemReaderBuilder<Pay>()
                .name(JOB_NAME+"Reader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT p FROM Pay p")
                .build();
    }

    private ItemProcessor<Pay, PayCopy> jpaMergeProcessor() {
        return PayCopy::new;
    }

    private JpaItemWriter<PayCopy> jpaMergeWriter() {
        JpaItemWriter<PayCopy> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }

}
