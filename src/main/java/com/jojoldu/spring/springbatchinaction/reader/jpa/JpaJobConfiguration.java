package com.jojoldu.spring.springbatchinaction.reader.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
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
public class JpaJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize = 10;

    @Bean
    public Job jpaJob() {
        return jobBuilderFactory.get("jpaJob")
                .start(jpaStep())
                .build();
    }

    @Bean
    public Step jpaStep() {
        return stepBuilderFactory.get("jpaStep")
                .<Store, StoreProduct>chunk(chunkSize)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Store> reader () {
        JpaPagingItemReader<Store> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT s FROM Store s");
        reader.setPageSize(chunkSize);
        return reader;
    }


    public ItemProcessor<Store, StoreProduct> processor() {
        return store -> {
            log.info(">>>>>>>>>> Processor ");
            return StoreProduct.builder()
                    .storeName(store.getName())
                    .productTotalPrice(store.sumProductsPrice())
                    .build();
        };
    }

    public JpaItemWriter<StoreProduct> writer() {
        JpaItemWriter<StoreProduct> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

}
