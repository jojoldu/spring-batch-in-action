package com.jojoldu.spring.springbatchinaction.reader.jdbc;

import com.jojoldu.spring.springbatchinaction.reader.Pay;
import com.jojoldu.spring.springbatchinaction.reader.PayRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jojoldu@gmail.com on 30/07/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcItemReaderJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public JdbcPagingItemReader<Pay> jdbcPagingItemReader() {
        return new JdbcPagingItemReaderBuilder<Pay>()
                .fetchSize(10)
                .dataSource(dataSource)
                .rowMapper(new PayRowMapper())
                .queryProvider(createQueryProvider())
                .name("jdbcPagingItemReader")
                .build();
    }

    private MySqlPagingQueryProvider createQueryProvider() {
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("id, amount, txName, txDateTime");
        queryProvider.setFromClause("from pay");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("id", Order.ASCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider;
    }

    @Bean
    public Step jdbcItemReaderStep() {
        return stepBuilderFactory.get("step1")
                .<Pay, Pay>chunk(10)
                .reader(jdbcPagingItemReader())
                .writer(list -> {
                    for (Pay pay: list) {
                        log.info("Current Pay={}", pay);
                    }
                }).build();
    }

    @Bean
    public Job jdbcItemReaderJob() {
        return jobBuilderFactory.get("jdbcItemReaderJob")
                .start(jdbcItemReaderStep())
                .build();
    }


}
