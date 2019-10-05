package com.jojoldu.spring.springbatchinaction.persistwriter;

import com.jojoldu.spring.springbatchinaction.TestBatchConfig;
import com.jojoldu.spring.springbatchinaction.reader.jdbc.Pay;
import com.jojoldu.spring.springbatchinaction.reader.jdbc.PayRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
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
@SpringBootTest
@ContextConfiguration(classes={JpaPersistWriterJobConfiguration.class, TestBatchConfig.class})
public class JpaPersistWriterJobConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PayRepository payRepository;

    @Autowired
    private PayCopyRepository payCopyRepository;

    @Test
    public void persistTest() throws Exception {
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
        assertThat(payCopyRepository.findAll().size(), is(10));
    }

}
