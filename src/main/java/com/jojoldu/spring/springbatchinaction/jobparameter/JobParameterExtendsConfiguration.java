package com.jojoldu.spring.springbatchinaction.jobparameter;

import com.jojoldu.spring.springbatchinaction.reader.jpa.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.EntityManagerFactory;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JobParameterExtendsConfiguration {
    public static final String JOB_NAME = "jobParameterExtendsBatch";
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    @Value("${chunkSize:1000}")
    private int chunkSize;

    @Bean(JOB_NAME+"_reader")
    @StepScope
    public JpaPagingItemReader<Product> reader() {
        return new JpaPagingItemReaderBuilder<Product>()
                .pageSize(chunkSize)
                .queryString("SELECT p FROM Product p")
                .build();
    }

}
