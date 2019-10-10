package com.jojoldu.spring.springbatchinaction.exam10;

import com.jojoldu.spring.springbatchinaction.TestBatchConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.time.LocalDate;

import static com.jojoldu.spring.springbatchinaction.exam10.BatchJpaTestConfiguration.FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jojoldu@gmail.com on 06/10/2019
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@RunWith(SpringRunner.class)
@SpringBatchTest
@ContextConfiguration(classes={BatchJdbcTestConfiguration.class, TestBatchConfig.class})
public class BatchJdbcUnitTestJobConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcPagingItemReader<SalesSum> batchJdbcUnitTestJobReader;

    @Autowired
    private DataSource dataSource;

    private JdbcOperations jdbcTemplate;

    private static final LocalDate ORDER_DATE = LocalDate.of(2019,10,6);

    public StepExecution getStepExecution() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("orderDate", ORDER_DATE.format(FORMATTER))
                .toJobParameters();

        return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @Before
    public void setUp() throws Exception {
        this.batchJdbcUnitTestJobReader.setDataSource(this.dataSource);
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
    }

    @After
    public void tearDown() throws Exception {
        this.jdbcTemplate.update("delete from sales");
        this.jdbcTemplate.update("delete from sales_sum");
    }

    @Test
    public void 기간내_Sales가_집계되어_SalesSum이된다() throws Exception {
        //given
        int amount1 = 1000;
        int amount2 = 500;
        int amount3 = 100;

        jdbcTemplate.update("insert into sales (order_date, amount, order_no) values (?, ?, ?)", ORDER_DATE, amount1, "1");
        jdbcTemplate.update("insert into sales (order_date, amount, order_no) values (?, ?, ?)", ORDER_DATE, amount2, "2");
        jdbcTemplate.update("insert into sales (order_date, amount, order_no) values (?, ?, ?)", ORDER_DATE, amount3, "3");

        //when
        batchJdbcUnitTestJobReader.open(new ExecutionContext());

        //then
        SalesSum read1 = batchJdbcUnitTestJobReader.read();
        assertThat(read1.getAmountSum()).isEqualTo(amount1+amount2+amount3);
    }


}
