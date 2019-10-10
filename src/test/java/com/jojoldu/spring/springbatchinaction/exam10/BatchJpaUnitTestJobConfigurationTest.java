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
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
@SpringBootTest(classes={BatchJpaTestConfiguration.class, TestBatchConfig.class})
public class BatchJpaUnitTestJobConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JpaPagingItemReader<SalesSum> batchUnitTestJobReader;

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private SalesSumRepository salesSumRepository;

    private static final LocalDate orderDate = LocalDate.of(2019,10,6);

    @After
    public void tearDown() throws Exception {
        salesRepository.deleteAllInBatch();
        salesSumRepository.deleteAllInBatch();
    }

    public StepExecution getStepExecution() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("orderDate", orderDate.format(FORMATTER))
                .toJobParameters();

        return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @Test
    public void 기간내_Sales가_집계되어_SalesSum이된다() throws Exception {
        //given
        int amount1 = 1000;
        int amount2 = 500;
        int amount3 = 100;

        salesRepository.save(new Sales(orderDate, amount1, "1"));
        salesRepository.save(new Sales(orderDate, amount2, "2"));
        salesRepository.save(new Sales(orderDate, amount3, "3"));

        //when
        batchUnitTestJobReader.open(new ExecutionContext());

        //then
        SalesSum read1 = batchUnitTestJobReader.read();
        assertThat(read1.getAmountSum()).isEqualTo(amount1+amount2+amount3);
    }


}
