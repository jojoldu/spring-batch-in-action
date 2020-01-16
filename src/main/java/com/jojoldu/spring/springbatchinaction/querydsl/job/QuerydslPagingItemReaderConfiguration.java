package com.jojoldu.spring.springbatchinaction.querydsl.job;

import com.jojoldu.spring.springbatchinaction.exam10.SalesSum;
import com.jojoldu.spring.springbatchinaction.persistwriter.PayCopy;
import com.jojoldu.spring.springbatchinaction.querydsl.reader.QuerydslPagingItemReader;
import com.jojoldu.spring.springbatchinaction.reader.jdbc.Pay;
import com.jojoldu.spring.springbatchinaction.reader.jpa.Product;
import com.jojoldu.spring.springbatchinaction.reader.jpa.ProductBackup;
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
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.jojoldu.spring.springbatchinaction.reader.jpa.QProduct.product;
import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Created by jojoldu@gmail.com on 06/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class QuerydslPagingItemReaderConfiguration {
    public static final DateTimeFormatter FORMATTER = ofPattern("yyyy-MM-dd");
    public static final String JOB_NAME = "querydslPagingReaderJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;
    private final QuerydslPagingItemReaderJobParameter jobParameter;

    private int chunkSize;

    @Value("${chunkSize:1000}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean
    @JobScope
    public QuerydslPagingItemReaderJobParameter jobParameter() {
        return new QuerydslPagingItemReaderJobParameter();
    }

    @Bean
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step())
                .build();
    }

    @Bean
    public Step step() {
        return stepBuilderFactory.get("querydslPagingReaderStep")
                .<Product, ProductBackup>chunk(chunkSize)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    public QuerydslPagingItemReader<Product> reader() {
        return new QuerydslPagingItemReader<>(emf, chunkSize, queryFactory -> queryFactory
                .selectFrom(product)
                .where(product.createDate.eq(jobParameter.getTxDate())));
    }

    private ItemProcessor<Product, ProductBackup> processor() {
        return ProductBackup::new;
    }

    @Bean
    public JpaItemWriter<ProductBackup> writer() {
        JpaItemWriter<ProductBackup> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(emf);
        return jpaItemWriter;
    }


}
