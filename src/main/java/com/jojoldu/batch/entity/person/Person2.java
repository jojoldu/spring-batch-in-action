package com.jojoldu.batch.entity.person;

/**
 * Created by jojoldu@gmail.com on 01/07/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@NoArgsConstructor
@Entity
public class Person2 {

    @Id
    private Long id;

    private String name;

    public Person2(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
