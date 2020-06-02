package com.jojoldu.batch.example.writer;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.pay.Pay;
import com.jojoldu.batch.entity.pay.Pay2Repository;
import com.jojoldu.batch.entity.pay.PayRepository;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@RunWith(SpringRunner.class)
@SpringBatchTest
@SpringBootTest(classes={JpaItemWriterJobConfiguration.class, TestBatchConfig.class})
public class JpaItemWriterJobConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PayRepository payRepository;

    @Autowired
    private Pay2Repository pay2Repository;

    @After
    public void tearDown() throws Exception {
        payRepository.deleteAllInBatch();
        pay2Repository.deleteAllInBatch();
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void JpaItemWriter테스트() throws Exception {
        //given
        for(long i=0;i<10;i++) {
            payRepository.save(new Pay(i*100, String.valueOf(i), LocalDateTime.now()));
        }
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("version", "1");

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(builder.toJobParameters());

        //then
        assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
        assertThat(pay2Repository.findAll().size(), is(10));
    }

}
