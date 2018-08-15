package com.jojoldu.spring.springbatchinaction.job;

import com.jojoldu.spring.springbatchinaction.TestJobLauncher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.is;

/**
 * Created by jojoldu@gmail.com on 15/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@TestPropertySource(properties = "job.name=simpleJob")
@RunWith(SpringRunner.class)
@SpringBootTest
public class JobParameterTest {

    @Autowired
    @Qualifier("simpleJob")
    private Job job;

    @Autowired
    private TestJobLauncher testJobLauncher;

    @Test
    public void JobParameter생성시점() throws Exception {
        //given
        JobLauncherTestUtils jobLauncherTestUtils = testJobLauncher.getJobLauncherTestUtils(job);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("requestDate", "20180815");

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(builder.toJobParameters());

        //then
        Assert.assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
    }


}
