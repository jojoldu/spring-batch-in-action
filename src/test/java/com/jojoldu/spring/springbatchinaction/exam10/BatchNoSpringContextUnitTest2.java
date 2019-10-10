package com.jojoldu.spring.springbatchinaction.exam10;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.util.Properties;

import static com.jojoldu.spring.springbatchinaction.exam10.BatchJpaTestConfiguration.FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * Created by jojoldu@gmail.com on 09/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
public class BatchNoSpringContextUnitTest2 {

    private DataSource dataSource;
    private ConfigurableApplicationContext context;
    private LocalDate orderDate;
    private BatchJdbcTestConfiguration job;

    @Before
    public void setUp() {
        this.context = new AnnotationConfigApplicationContext(TestDataSourceConfiguration.class);
        this.dataSource = (DataSource) context.getBean("dataSource");
        this.orderDate = LocalDate.of(2019, 10, 6);
        this.job = new BatchJdbcTestConfiguration(null, null, dataSource);
        job.setChunkSize(10);
    }

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void 기간내_Sales가_집계되어_SalesSum이된다() throws Exception {
        JdbcPagingItemReader<SalesSum> reader = job.batchJdbcUnitTestJobReader(orderDate.format(FORMATTER));
        reader.afterPropertiesSet();

        ExecutionContext executionContext = new ExecutionContext();
        reader.open(executionContext);
        assertThat(reader.read().getAmountSum()).isEqualTo(1110);
        assertNull(reader.read());
    }

    @Configuration
    public static class TestDataSourceConfiguration {

        private static final String CREATE_SQL =
                        "create table sales (id bigint not null auto_increment, amount bigint not null, order_date date, order_no varchar(255), primary key (id)) engine=InnoDB;";

        private static final String INSERT_SQL =
                        "insert into sales (order_date, amount, order_no) values ('2019-10-06', 1000, '1');" +
                        "insert into sales (order_date, amount, order_no) values ('2019-10-06', 100, '2');" +
                        "insert into sales (order_date, amount, order_no) values ('2019-10-06', 10, '3');";

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory();
            databaseFactory.setDatabaseType(H2);
            return databaseFactory.getDatabase();
        }

        @Bean
        public DataSourceInitializer initializer(DataSource dataSource) {
            DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
            dataSourceInitializer.setDataSource(dataSource);

            Resource create = new ByteArrayResource(CREATE_SQL.getBytes());
            Resource insert = new ByteArrayResource(INSERT_SQL.getBytes());
            dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create, insert));

            return dataSourceInitializer;
        }
    }
}
