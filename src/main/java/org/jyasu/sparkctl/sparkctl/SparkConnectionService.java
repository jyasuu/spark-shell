package org.jyasu.sparkctl.sparkctl;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.Properties;

@Service
public class SparkConnectionService {

    private final SparkConnectionConfig config;
    private Connection connection;

    public SparkConnectionService(SparkConnectionConfig config) {
        this.config = config;
    }

    public boolean connect() throws SQLException {
        return connect(config.getHost(), config.getPort(), config.getDatabase(),
                config.getUsername(), config.getPassword());
    }

    public boolean connect(String host, int port, String database, String username, String password) throws SQLException {
        disconnect();
        config.setHost(host);
        config.setPort(port);
        config.setDatabase(database);
        config.setUsername(username);
        config.setPassword(password);

        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Hive JDBC driver not found", e);
        }

        Properties props = new Properties();
        if (username != null && !username.isBlank()) props.setProperty("user", username);
        if (password != null && !password.isBlank()) props.setProperty("password", password);

        connection = DriverManager.getConnection(config.getJdbcUrl(), props);
        return connection != null && !connection.isClosed();
    }

    public void disconnect() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public QueryResult executeQuery(String sql) throws SQLException {
        ensureConnected();
        try (Statement stmt = connection.createStatement()) {
            long start = System.currentTimeMillis();
            boolean hasResults = stmt.execute(sql);
            long elapsed = System.currentTimeMillis() - start;

            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    return QueryResult.fromResultSet(rs, elapsed);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                return QueryResult.fromUpdateCount(updateCount, elapsed);
            }
        }
    }

    public SparkConnectionConfig getConfig() {
        return config;
    }

    private void ensureConnected() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Not connected to Spark. Use 'connect' command first.");
        }
    }
}
