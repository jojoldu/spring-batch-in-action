package com.jojoldu.batch.example.socketclose;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jojoldu@gmail.com on 11/09/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class SocketCloseTaskletJobConfiguration {
    private static final String BEAN_PREFIX = "socketCloseTasklet";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean(BEAN_PREFIX+"_job")
    public Job job(){
        return jobBuilderFactory.get(BEAN_PREFIX+"_job")
                .start(step())
                .build();
    }

    @Bean(BEAN_PREFIX+"_step")
    public Step step() {
        return stepBuilderFactory.get(BEAN_PREFIX+"_step")
                .tasklet((contribution, chunkContext) -> {
                    Thread.sleep(60_000); // Tasklet만으로 60초
                    return RepeatStatus.FINISHED;
                })
                .build();
    }


}
