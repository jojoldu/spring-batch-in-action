package com.jojoldu.batch.example.supplierreader;

import com.jojoldu.batch.entity.student.QTeacher;
import com.jojoldu.batch.entity.student.Teacher;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by jojoldu@gmail.com on 19/02/2021
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */

@RequiredArgsConstructor
@Repository
public class QuerydslSupplierRepository {
    private final JPAQueryFactory queryFactory;

    public List<Teacher> findAllByPaging(String name, int offset, int limit) {
        QTeacher teacher = QTeacher.teacher;
        return queryFactory
                .selectFrom(teacher)
                .where(teacher.name.eq(name))
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<Teacher> findAllByNewTxPaging(String name, int offset, int limit) {
        QTeacher teacher = QTeacher.teacher;
        return queryFactory
                .selectFrom(teacher)
                .where(teacher.name.eq(name))
                .offset(offset)
                .limit(limit)
                .fetch();
    }
}

