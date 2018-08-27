package com.jojoldu.spring.springbatchinaction.reader.file;

import com.jojoldu.spring.springbatchinaction.reader.jdbc.Pay;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 * Created by jojoldu@gmail.com on 30/07/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

public class PayFieldSetMapper implements FieldSetMapper<Pay> {

    @Override
    public Pay mapFieldSet(FieldSet fieldSet) throws BindException {
        return new Pay(fieldSet.readLong("amount"),
                fieldSet.readString("txName"),
                fieldSet.readString("txDateTime"));
    }
}
