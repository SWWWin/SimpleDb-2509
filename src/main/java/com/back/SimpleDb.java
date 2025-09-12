package com.back;



import java.sql.*;

/*
HikariCP: 자바 진영에서 가장 널리 쓰이는 jdbc 커넥션 풀 라이브러리
-> db 커넥션을 효율적으로 관리해주는 풀 관리자

DiriverManager.getConnection(...)을 매번 호출하면  소켓 연결 -> 로그인 인증 -> 세션 생성 비용이 크다

커넥션 풀은 이런 문제를 해결하기 위해 여러개의 DB Connection을 만들어 두고 필요할 때 꺼내 쓰고 다 쓰면 반납하는 공용 커넥션 창고
 */


public class SimpleDb {

    private final String url;
    private final String user;
    private final String password;
    private boolean mode;

    public SimpleDb(String host, String user, String password, String dbName) {
        this.url = "jdbc:mysql://" + host + "/" + dbName + "?serverTimezone=Asia/Seoul";
        this.user = user;
        this.password = password;


    }

    public void setDevMode(boolean mode) {
        this.mode = mode;
    }


    // SQL 한번 실행
    public void run(String sql, Object ... values) {
        try (
            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            ps.executeUpdate();
        } 
        catch (SQLException e) {
            throw new RuntimeException("SQL 실행 오류: " + e.getMessage(), e);
        }

    }

    // sql 객체 반환
    public Sql genSql() {
        try {
            return new Sql(DriverManager.getConnection(url, user, password));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
