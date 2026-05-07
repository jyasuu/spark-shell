package org.jyasu.sparkctl.sparkctl;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spark")
public class SparkConnectionConfig {

    private String host = "localhost";
    private int port = 10000;
    private String database = "default";
    private String username = "";
    private String password = "";

    public String getJdbcUrl() {
        return String.format("jdbc:hive2://%s:%d/%s", host, port, database);
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
