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
        sb.append(part);
        for(Object v: values) params.add(v);
        return this;
    }

    public long insert() {
        try (PreparedStatement ps =
                     connection.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
            bind(ps);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
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
            ps.setObject(i + 1, params.get(i));
        }
    }

    private void close() {
        try { connection.close(); } catch (SQLException ignore) {}
    }

}
