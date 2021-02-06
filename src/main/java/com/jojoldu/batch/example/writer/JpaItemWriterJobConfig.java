package com.jojoldu.batch.example.writer;

import com.jojoldu.batch.entity.student.Student;
import com.jojoldu.batch.entity.student.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaItemWriterJobConfig {
    public static final String JOB_NAME = "jpaItemWriterJob";
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize;

    @Value("${chunkSize:10}")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean(name = JOB_NAME)
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step())
                .build();
    }

    @Bean(name = JOB_NAME +"_step")
    public Step step() {
        return stepBuilderFactory.get(JOB_NAME +"_step")
                .<Teacher, Teacher>chunk(chunkSize)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public JpaPagingItemReader<Teacher> reader() {
        return new JpaPagingItemReaderBuilder<Teacher>()
                .name(JOB_NAME +"_reader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT t FROM Teacher t")
                .build();
    }

    public ItemProcessor<Teacher, Teacher> processor() {
        return teacher -> {
            log.info("teacher.id={}", teacher.getId());
            teacher.addStudent(new Student("신규 제자"));
            return teacher;
        };
    }

    public JpaItemWriter<Teacher> writer() {
        return new JpaItemWriterBuilder<Teacher>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}

