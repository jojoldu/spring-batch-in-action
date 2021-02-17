package com.jojoldu.batch.example.reader;

import com.jojoldu.batch.TestBatchConfig;
import com.jojoldu.batch.entity.student.Student;
import com.jojoldu.batch.entity.student.Teacher;
import com.jojoldu.batch.entity.student.TeacherRepository;
import com.jojoldu.batch.example.reader.hibernate.HibernateCursorItemReaderJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
@SpringBootTest(classes = {HibernateCursorItemReaderJobConfig.class, TestBatchConfig.class})
class HibernateCursorItemReaderJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private TeacherRepository teacherRepository;

    @AfterEach
    void tearDown() {
        teacherRepository.deleteAll();
    }

    @SuppressWarnings("Duplicates")
    @DisplayName("hibernateCursor테스트")
    @Test
    void hibernate_cursor_test() throws Exception {
        //given
        for(long i=1;i<=10;i++) {
            String teacherName = i + "선생님";
            Teacher teacher = new Teacher(teacherName, "수학");
            teacher.addStudent(new Student(teacherName+"의 제자1"));
            teacher.addStudent(new Student(teacherName+"의 제자2"));
            teacherRepository.save(teacher);
        }

        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
