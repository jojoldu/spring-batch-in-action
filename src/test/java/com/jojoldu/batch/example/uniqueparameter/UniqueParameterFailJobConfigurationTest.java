package com.jojoldu.batch.example.uniqueparameter;

import com.jojoldu.batch.TestBatchConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@ExtendWith(SpringExtension.class)
@SpringBatchTest
@SpringBootTest(classes={UniqueParameterJobConfiguration.class, TestBatchConfig.class})
public class UniqueParameterFailJobConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void A_실패테스트() throws Exception {
        //given
        String requestDate = "2020-03-09";
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("requestDate", requestDate);

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(builder.toJobParameters());

        //then
        assertThat(jobExecution.getStatus(), is(BatchStatus.FAILED));
    }

    @Test
    public void B_성공테스트() throws Exception {
        //given
        String requestDate = "2020-03-10";
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("requestDate", requestDate);

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(builder.toJobParameters());

        //then
        assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
    }
}
