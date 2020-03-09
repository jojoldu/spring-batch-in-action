package com.jojoldu.spring.springbatchinaction.uniqueparameter;

import com.jojoldu.spring.springbatchinaction.TestBatchConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@RunWith(SpringRunner.class)
@SpringBatchTest
@SpringBootTest(classes={UniqueParameterJobConfiguration.class, TestBatchConfig.class})
public class UniqueParameterFailJobConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @SpyBean
    private UniqueParameterJobConfiguration jobConfiguration;

    @Test
    public void 실패테스트() throws Exception {
        //given
        String requestDate = "2020-03-09";
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("requestDate", requestDate);

        // 강제로 배치를 실패시킨다.
        given(jobConfiguration.getMessage(anyString())).willThrow(new IllegalStateException());

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(builder.toJobParameters());

        //then
        assertThat(jobExecution.getStatus(), is(BatchStatus.FAILED));
    }

    @Test
    public void 성공테스트() throws Exception {
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
