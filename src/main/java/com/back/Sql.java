package com.back;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Sql {
    private final Connection connection;
    private final StringBuilder sb = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(Connection connection) {
        this.connection = connection;
    }

    public Sql append(String part, Object... values) {
        if(sb.length() > 0) sb.append(" ");
        sb.append(part); // sql문 저장
        for(Object v: values) params.add(v); // 타입 저장
        return this;
    }

    /*
    DriverManager → Connection → Statement/PreparedStatement → (파라미터 바인딩) →
    실행(executeQuery / executeUpdate) → ResultSet(SELECT 시) → 자원 해제 순서로 실행

    PreparedStatement란?
    JDBC에서 SQL을 실행할 때 사용하는 미리 컴파일된 SQL 객체


     */
    public long insert() {
        // DriverManager로 얻은 DB연결
        // Statement.RETURN_GENERATED_KEYS: 생성된 PK 반환 옵션

        try (PreparedStatement ps =
                     connection.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
            bind(ps);
            ps.executeUpdate(); // Insert 쿼리 실행
            try (ResultSet rs = ps.getGeneratedKeys()) { // DB가 방금 생성한 키를 ResultSet 형태로 반환
                if (rs.next()) return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    private void bind(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            /*
            PreparedStatement에서의 ?
            INSERT INTO article SET title = ?, body = ?, isBlind = ?

            setOject(parameterIndex, value)
            parameterIndex: 몇 번쨰 ?인지
            value: 그 ?의 값
             */
            ps.setObject(i + 1, params.get(i));
        }
    }

    private void close() {
        try { connection.close(); } catch (SQLException ignore) {}
    }

}
