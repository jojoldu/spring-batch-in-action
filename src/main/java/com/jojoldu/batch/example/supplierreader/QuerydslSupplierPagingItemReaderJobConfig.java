package com.jojoldu.batch.example.supplierreader;

import com.jojoldu.batch.entity.student.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class QuerydslSupplierPagingItemReaderJobConfig {
    public static final String JOB_NAME = "querydslSupplierPagingItemReaderJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final QuerydslSupplierRepository querydslSupplierRepository;

    private int chunkSize;

    @Value("${chunkSize:5}")
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
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean(name = JOB_NAME +"_reader")
    @StepScope
    public QuerydslSupplierPagingItemReader<Teacher> reader(@Value("#{jobParameters[name]}") String name) {
        return QuerydslSupplierPagingItemReader.<Teacher>builder()
                .pageSize(chunkSize)
                .repositorySupplier((offset, limit) -> querydslSupplierRepository.findAllByPaging(name, offset, limit))
                .build();
    }

    public ItemProcessor<Teacher, Teacher> processor() {
        return teacher -> {
            log.info("students count={}", teacher.getStudents().size());
            return teacher;
        };
    }

    private ItemWriter<Teacher> writer() {
        return list -> {
            for (Teacher teacher: list) {
                log.info("Current Teacher={}", teacher);
            }
        };
    }
}
