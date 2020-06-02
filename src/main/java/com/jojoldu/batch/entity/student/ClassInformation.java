package com.jojoldu.batch.entity.student;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Created by jojoldu@gmail.com on 2018. 10. 19.
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@ToString
@Getter
@NoArgsConstructor
public class ClassInformation {

    private String teacherName;
    private int studentSize;

    public ClassInformation(String teacherName, int studentSize) {
        this.teacherName = teacherName;
        this.studentSize = studentSize;
    }
}
