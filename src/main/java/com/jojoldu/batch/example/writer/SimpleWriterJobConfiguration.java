package com.jojoldu.batch.example.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jojoldu@gmail.com on 30/07/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SimpleWriterJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @StepScope
    public ListItemReader<Integer> simpleWriterReader() {
        List<Integer> items = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            items.add(i);
        }

        return new ListItemReader<>(items);
    }

    @Bean
    public PrintItemWriter simpleWriter() {
        return new PrintItemWriter();
    }

    @Bean
    public Step simpleWriterStep() {
        return stepBuilderFactory.get("simpleWriterStep")
                .<Integer, Integer>chunk(10)
                .reader(simpleWriterReader())
                .writer(simpleWriter())
                .build();
    }

    @Bean
    public Job simpleWriterJob() {
        return jobBuilderFactory.get("simpleWriterJob")
                .start(simpleWriterStep())
                .build();
    }
}
