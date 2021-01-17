package com.jojoldu.batch.example.socketclose;

import com.jojoldu.batch.entity.product.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Created by jojoldu@gmail.com on 11/09/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class SocketCloseSlowNoTxBatch {
    private static final String BEAN_PREFIX = "SocketCloseSlowNoTxBatch";
    private static final int chunkSize = 1;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource; // DataSource DI

    @Bean(BEAN_PREFIX+"_job")
    public Job job() throws Exception {
        return jobBuilderFactory.get(BEAN_PREFIX+"_job")
                .start(step())
                .build();
    }

    @Bean(BEAN_PREFIX+"_step")
    public Step step() throws Exception {
        return stepBuilderFactory.get(BEAN_PREFIX+"_step")
                .<Store, Store>chunk(chunkSize)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .transactionManager(new ResourcelessTransactionManager()) // No Transaction
                .build();
    }

//    @Bean(BEAN_PREFIX+"_reader")
//    public JdbcPagingItemReader<Store> reader() throws Exception {
//        Map<String, Object> params = new HashMap<>();
//        params.put("name", "jojoldu");
//
//        return new JdbcPagingItemReaderBuilder<Store>()
//                .pageSize(chunkSize)
//                .fetchSize(chunkSize)
//                .dataSource(dataSource)
//                .rowMapper(new BeanPropertyRowMapper<>(Store.class))
//                .queryProvider(queryProvider())
//                .parameterValues(params)
//                .name(BEAN_PREFIX+"_reader")
//                .build();
//    }

    @Bean(BEAN_PREFIX+"_reader")
    public ListItemReader<Store> reader() throws Exception {
        return new ListItemReader<>(Arrays.asList(new Store("jojoldu")));
    }

    public ItemProcessor<Store, Store> processor() {
        return item -> {
            log.info("processor start");
            Thread.sleep(150_000);// 2.5% 버퍼 대비 넉넉하게 100초로
            log.info("processor end");
            return item;
        };
    }

//    @Bean(BEAN_PREFIX+"_writer")
//    public ItemWriter<Store> writer() {
//        return items -> log.info("items.size={}", items.size());
//    }

    @Bean(BEAN_PREFIX+"_writer")
    public JdbcBatchItemWriter<Store> writer() {
        return new JdbcBatchItemWriterBuilder<Store>()
                .dataSource(dataSource)
                .sql("insert into store(name) values (:name)")
                .beanMapped()
                .build();
    }

    @Bean(name = BEAN_PREFIX+"_queryProvider")
    public PagingQueryProvider queryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, name");
        queryProvider.setFromClause("FROM store");
        queryProvider.setWhereClause("WHERE name=:name");
        queryProvider.setSortKey("id");

        return queryProvider.getObject();
    }
}
