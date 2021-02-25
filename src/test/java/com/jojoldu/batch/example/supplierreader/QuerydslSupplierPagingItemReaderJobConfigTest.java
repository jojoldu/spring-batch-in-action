package com.jojoldu.batch.example.supplierreader;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.student.Student;
import com.jojoldu.batch.entity.student.Teacher;
import com.jojoldu.batch.entity.student.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = {
        QuerydslSupplierPagingItemReaderJobConfig.class,
        TestBatchConfig.class,
        QuerydslSupplierRepository.class
})
class QuerydslSupplierPagingItemReaderJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private TeacherRepository teacherRepository;

    @AfterEach
    void tearDown() {
        teacherRepository.deleteAll();
    }

    @Test
    void querydsl_paging_test() throws Exception {
        //given
        String teacherName = "선생님";
        for(long i=1;i<=10;i++) {
            Teacher teacher = new Teacher(teacherName, "수학");
            teacher.addStudent(new Student(teacherName+"의 제자1"));
            teacher.addStudent(new Student(teacherName+"의 제자2"));
            teacherRepository.save(teacher);
        }

        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
                .addString("name", teacherName)
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void querydsl_requirenew_paging_test() throws Exception {
        //given
        String teacherName = "선생님";
        for(long i=1;i<=10;i++) {
            Teacher teacher = new Teacher(teacherName, "수학");
            teacher.addStudent(new Student(teacherName+"의 제자1"));
            teacher.addStudent(new Student(teacherName+"의 제자2"));
            teacherRepository.save(teacher);
        }

        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
                .addString("name", teacherName)
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}

