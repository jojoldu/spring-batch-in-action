package com.jojoldu.spring.springbatchinaction.writer;

import com.jojoldu.spring.springbatchinaction.TestJobLauncher;
import com.jojoldu.spring.springbatchinaction.reader.jdbc.Pay;
import com.jojoldu.spring.springbatchinaction.reader.jdbc.Pay2Repository;
import com.jojoldu.spring.springbatchinaction.reader.jdbc.PayRepository;
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
@SpringBootTest
public class JpaItemWriterJobConfigurationTest {

    @Autowired
    @Qualifier("jpaItemWriterJob")
    private Job job;

    @Autowired
    private TestJobLauncher testJobLauncher;

    @Autowired
    private PayRepository payRepository;

    @Autowired
    private Pay2Repository pay2Repository;

    @SuppressWarnings("Duplicates")
    @Test
    public void JdbcItemWriter테스트() throws Exception {
        //given
        for(long i=0;i<10;i++) {
            payRepository.save(new Pay(i*100, String.valueOf(i), LocalDateTime.now()));
        }
        JobLauncherTestUtils jobLauncherTestUtils = testJobLauncher.getJobLauncherTestUtils(job);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("version", "1");

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(builder.toJobParameters());

        //then
        assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
        assertThat(pay2Repository.findAll().size(), is(10));
    }

}
