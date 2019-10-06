package com.jojoldu.spring.springbatchinaction.exam10;

import com.jojoldu.spring.springbatchinaction.reader.jdbc.Pay2;
import com.jojoldu.spring.springbatchinaction.reader.jpa.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Created by jojoldu@gmail.com on 06/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class BatchUnitTestConfiguration {
    public static final String JOB_NAME = "batchUnitTestJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;

    @Value("${chunkSize:1000}")
    private int chunkSize;

    @Bean
    public Job batchUnitTestJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(batchUnitTestJobStep())
                .build();
    }

    @Bean
    public Step batchUnitTestJobStep() {
        return stepBuilderFactory.get("batchUnitTestJobStep")
                .<Sales, SalesSum>chunk(chunkSize)
                .reader(batchUnitTestJobReader(null, null))
                .writer(batchUnitTestJobWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Sales> batchUnitTestJobReader(
            @Value("#{jobParameters[startDate]}") String startDate,
            @Value("#{jobParameters[endDate]}") String endDate) {

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", LocalDate.parse(startDate, ofPattern("yyyy-MM-dd")));
        params.put("endDate", LocalDate.parse(endDate, ofPattern("yyyy-MM-dd")));

        String className = SalesSum.class.getName(); // JPQL 에서 새로운 Entity로 집계하기 위해
        String queryString = String.format (
                        "SELECT new %s(:startDate, :endDate, SUM(s.amount)) " +
                        "FROM Sales s " +
                        "WHERE s.orderDate BETWEEN :startDate AND :endDate", className);

        return new JpaPagingItemReaderBuilder<Sales>()
                .name("batchUnitTestJobReader")
                .entityManagerFactory(emf)
                .pageSize(chunkSize)
                .queryString(queryString)
                .parameterValues(params)
                .build();
    }

    @Bean
    public JpaItemWriter<SalesSum> batchUnitTestJobWriter() {
        JpaItemWriter<SalesSum> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(emf);
        return jpaItemWriter;
    }

}
