package com.jojoldu.spring.springbatchinaction;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by jojoldu@gmail.com on 15/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

public class TestJobLauncher {
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JobLauncher jobLauncher;

    public JobLauncherTestUtils getJobLauncherTestUtils(Job job) {
        JobLauncherTestUtils jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(job);
        return jobLauncherTestUtils;
    }
}
