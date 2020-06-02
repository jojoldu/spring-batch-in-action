package com.jojoldu.batch.example.exam10;

import com.jojoldu.batch.entity.sales.SalesSum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
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

import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Created by jojoldu@gmail.com on 06/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@Slf4j // log 사용을 위한 lombok 어노테이션
@RequiredArgsConstructor // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class BatchJpaTestConfiguration {
    public static final DateTimeFormatter FORMATTER = ofPattern("yyyy-MM-dd");
    public static final String JOB_NAME = "batchJpaUnitTestJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;

    @Value("${chunkSize:1000}")
    private int chunkSize;

    @Bean
    public Job batchJpaUnitTestJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(batchJpaUnitTestJobStep())
                .build();
    }

    @Bean
    public Step batchJpaUnitTestJobStep() {
        return stepBuilderFactory.get("batchJpaUnitTestJobStep")
                .<SalesSum, SalesSum>chunk(chunkSize)
                .reader(batchJpaUnitTestJobReader(null))
                .writer(batchJpaUnitTestJobWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<SalesSum> batchJpaUnitTestJobReader(
            @Value("#{jobParameters[orderDate]}") String orderDate) {

        Map<String, Object> params = new HashMap<>();

        params.put("orderDate", LocalDate.parse(orderDate, FORMATTER));

        String className = SalesSum.class.getName(); // JPQL 에서 새로운 Entity로 반환하기 위해
        String queryString = String.format (
                        "SELECT new %s(s.orderDate, SUM(s.amount)) " +
                        "FROM Sales s " +
                        "WHERE s.orderDate =:orderDate " +
                        "GROUP BY s.orderDate ", className);

        return new JpaPagingItemReaderBuilder<SalesSum>()
                .name("batchJpaUnitTestJobReader")
                .entityManagerFactory(emf)
                .pageSize(chunkSize)
                .queryString(queryString)
                .parameterValues(params)
                .build();
    }

    @Bean
    public JpaItemWriter<SalesSum> batchJpaUnitTestJobWriter() {
        JpaItemWriter<SalesSum> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(emf);
        return jpaItemWriter;
    }

}
