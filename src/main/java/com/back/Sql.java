package com.back;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
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

    public int update() {
        try (PreparedStatement ps =
                     connection.prepareStatement(sb.toString())) {
            bind(ps);
            return ps.executeUpdate(); // 수정된 row 갯수 밴환
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    public int delete() {
        try (PreparedStatement ps =
                     connection.prepareStatement(sb.toString())) {
            bind(ps);
            return ps.executeUpdate(); // 삭제된 row 갯수 밴환
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }


    public <T> T selectRow(Class<T> clazz) {
        List<T> rows = selectRows(clazz);
        return rows.isEmpty() ? null : rows.get(0);//원하는 행 가지고 오기
    }


    public <T> List<T> selectRows(Class<T> clazz) {
        List<T> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sb.toString())) {
            bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    T obj = clazz.getDeclaredConstructor().newInstance();
                    for (int i = 1; i <= columnCount; i++) {
                        String column = meta.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        String fieldName = toCamelCase(column);
                        try {
                            Field field = clazz.getDeclaredField(fieldName);
                            field.setAccessible(true);
                            if (value instanceof Timestamp ts
                                    && field.getType().equals(LocalDateTime.class)) {
                                field.set(obj, ts.toLocalDateTime());
                            } else {
                                field.set(obj, value);
                            }
                        } catch (NoSuchFieldException ignore) {}
                    }
                    results.add(obj);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
        return results;
    }

    private String toCamelCase(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = false;
        }
        return sb.toString();
    }

    public LocalDateTime selectDatetime() {
        return LocalDateTime.now();
    }

    public Long selectLong() {
        try(PreparedStatement ps = connection.prepareStatement(sb.toString())) { // 현재 누적된 SQL 실행
            bind(ps);
            try(ResultSet rs = ps.executeQuery()) { // 맨 첫 행 이동
                if(rs.next()) {
                    Object value = rs.getObject(1);
                    return value == null? null : ((Number) value).longValue(); // 원하는 id가 없는 경우 null, 있는 경우 출력
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    public String selectString() {
        try(PreparedStatement ps = connection.prepareStatement(sb.toString())) { // 현재 누적된 SQL 실행
            bind(ps);
            try(ResultSet rs = ps.executeQuery()) { // 맨 첫 행 이동
                if(rs.next()) {
                    Object value = rs.getObject(1);
                    return value == null? null : value.toString(); // 원하는 제목이 없는 경우 null, 있는 경우 제목 그대로 출력
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    public Boolean selectBoolean() {
        try(PreparedStatement ps = connection.prepareStatement(sb.toString())) { // 현재 누적된 SQL 실행
            bind(ps);
            try(ResultSet rs = ps.executeQuery()) { // 맨 첫 행 이동
                if(rs.next()) {
                    Object value = rs.getObject(1);
                    if(value == null) return null; //null인지 확인
                    if(value instanceof Boolean) return (Boolean) value; // boolean인지 확인
                    if(value instanceof Number) return ((Number) value).intValue() != 0; // BIT(), TINYINT()로 저장된 경우 -> Boolean 변환
                    return Boolean.parseBoolean(value.toString());
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    public Sql appendIn(String part, Object ... values) {
        if (values == null || values.length == 0) //null 처리
            throw new IllegalArgumentException("Values required");


        // ?를 values,length만큼 만들어서 치환
        StringBuilder placeHolders = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if(i > 0) placeHolders.append(", ");
            placeHolders.append("?");
            this.params.add(values[i]);
        }

        if(sb.length() > 0) sb.append(" "); //공백 추가
        //part 문자열에 있는 ? 하나를 (?, ?, ...) 형태로 바꿔줌
        sb.append(part.replace("?", placeHolders.toString()));


        return this;


    }

    public List<Long> selectLongs() {
        List<Long> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sb.toString())) {
            bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object value = rs.getObject(1);
                    results.add(value == null ? null : ((Number) value).longValue());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
        return results;
    }
}
