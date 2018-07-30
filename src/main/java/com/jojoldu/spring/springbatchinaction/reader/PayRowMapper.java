package com.jojoldu.spring.springbatchinaction.reader;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jojoldu@gmail.com on 30/07/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

public class PayRowMapper implements RowMapper<Pay> {

    @Override
    public Pay mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Pay(rs.getLong("id"),
                rs.getLong("amount"),
                rs.getString("txName"),
                rs.getString("txDateTime"));
    }
}
