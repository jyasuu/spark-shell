package org.jyasu.sparkctl.sparkctl;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ShellComponent
public class SparkShellCommands {

    private final SparkConnectionService connectionService;

    public SparkShellCommands(SparkConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    // ─── Connection management ────────────────────────────────────────────────

    @ShellMethod(value = "Connect to Spark Thrift Server via Hive JDBC", key = "connect")
    public String connect(
            @ShellOption(value = "--host",     defaultValue = ShellOption.NULL) String host,
            @ShellOption(value = "--port",     defaultValue = "0")              int port,
            @ShellOption(value = "--database", defaultValue = ShellOption.NULL) String database,
            @ShellOption(value = "--username", defaultValue = ShellOption.NULL) String username,
            @ShellOption(value = "--password", defaultValue = ShellOption.NULL) String password
    ) {
        SparkConnectionConfig cfg = connectionService.getConfig();
        String h  = host     != null ? host     : cfg.getHost();
        int    p  = port > 0 ? port             : cfg.getPort();
        String db = database != null ? database : cfg.getDatabase();
        String u  = username != null ? username : cfg.getUsername();
        String pw = password != null ? password : cfg.getPassword();

        try {
            connectionService.connect(h, p, db, u, pw);
            return String.format("✔ Connected to %s:%d / %s", h, p, db);
        } catch (SQLException e) {
            return "✘ Connection failed: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Disconnect from Spark", key = "disconnect")
    public String disconnect() {
        connectionService.disconnect();
        return "Disconnected.";
    }

    @ShellMethod(value = "Show current connection status", key = "status")
    public String status() {
        if (connectionService.isConnected()) {
            SparkConnectionConfig cfg = connectionService.getConfig();
            return "✔ Connected  →  " + cfg.getJdbcUrl();
        }
        return "✘ Not connected. Use 'connect' to establish a connection.";
    }

    // ─── Inline SQL ───────────────────────────────────────────────────────────

    @ShellMethod(value = "Execute a SQL query against Spark", key = "sql")
    public String sql(
            @ShellOption(value = "--query", help = "SQL statement to execute") String query
    ) {
        return runSql(query.trim());
    }

    // ─── File-based SQL ───────────────────────────────────────────────────────

    @ShellMethod(value = "Execute SQL statements from a .sql file", key = "exec-file")
    public String execFile(
            @ShellOption(value = "--file", help = "Path to the .sql file") String filePath,
            @ShellOption(value = "--stop-on-error", defaultValue = "true",
                         help = "Stop execution on first error (default: true)") boolean stopOnError
    ) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return "✘ File not found: " + filePath;
        }
        if (!Files.isReadable(path)) {
            return "✘ File is not readable: " + filePath;
        }

        String content;
        try {
            content = Files.readString(path);
        } catch (IOException e) {
            return "✘ Failed to read file: " + e.getMessage();
        }

        List<String> statements = splitStatements(content);
        if (statements.isEmpty()) {
            return "⚠ No SQL statements found in: " + filePath;
        }

        StringBuilder output = new StringBuilder();
        output.append(String.format("▶ Executing %d statement(s) from: %s%n%n", statements.size(), filePath));

        int succeeded = 0;
        int failed    = 0;

        for (int i = 0; i < statements.size(); i++) {
            String stmt = statements.get(i);
            output.append(String.format("── Statement [%d/%d] ──────────────────────────%n", i + 1, statements.size()));
            output.append(stmt).append("\n");

            String result = runSql(stmt);
            output.append(result).append("\n");

            if (result.startsWith("✘")) {
                failed++;
                if (stopOnError) {
                    output.append(String.format("%n⛔ Stopped after error (--stop-on-error=true).%n"));
                    output.append(String.format("   Completed: %d  Failed: %d  Skipped: %d%n",
                            succeeded, failed, statements.size() - i - 1));
                    return output.toString();
                }
            } else {
                succeeded++;
            }
        }

        output.append(String.format("✔ Done — %d succeeded, %d failed.%n", succeeded, failed));
        return output.toString();
    }

    // ─── Convenience commands ─────────────────────────────────────────────────

    @ShellMethod(value = "Show databases", key = "show-databases")
    public String showDatabases() { return runSql("SHOW DATABASES"); }

    @ShellMethod(value = "Show tables in current database", key = "show-tables")
    public String showTables() { return runSql("SHOW TABLES"); }

    @ShellMethod(value = "Describe a table's schema", key = "desc")
    public String describeTable(
            @ShellOption(value = "--table") String table
    ) { return runSql("DESCRIBE " + table); }

    @ShellMethod(value = "Use (switch to) a database", key = "use")
    public String useDatabase(
            @ShellOption(value = "--database") String database
    ) { return runSql("USE " + database); }

    @ShellMethod(value = "Show current database", key = "current-db")
    public String currentDatabase() { return runSql("SELECT current_database()"); }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String runSql(String sql) {
        if (!connectionService.isConnected()) {
            return "✘ Not connected. Run 'connect' first.";
        }
        try {
            QueryResult result = connectionService.executeQuery(sql);
            return TableRenderer.render(result);
        } catch (SQLException e) {
            return "✘ SQL error: " + e.getMessage();
        }
    }

    /**
     * Split file content into individual SQL statements by semicolons,
     * ignoring semicolons inside single-quoted strings and -- line comments.
     */
    private List<String> splitStatements(String content) {
        List<String> statements = new ArrayList<>();
        StringBuilder current   = new StringBuilder();
        boolean inString        = false;
        boolean inLineComment   = false;
        char[]  chars           = content.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            // Handle line comment start
            if (!inString && !inLineComment && c == '-' && i + 1 < chars.length && chars[i + 1] == '-') {
                inLineComment = true;
                current.append(c);
                continue;
            }
            // End of line comment
            if (inLineComment && c == '\n') {
                inLineComment = false;
                current.append(c);
                continue;
            }
            if (inLineComment) {
                current.append(c);
                continue;
            }

            // Toggle string mode on single quote (handle escaped '')
            if (c == '\'' && !inString) {
                inString = true;
                current.append(c);
                continue;
            }
            if (c == '\'' && inString) {
                // Check for escaped quote ''
                if (i + 1 < chars.length && chars[i + 1] == '\'') {
                    current.append(c);
                    current.append(chars[++i]);
                } else {
                    inString = false;
                    current.append(c);
                }
                continue;
            }

            // Statement delimiter
            if (c == ';' && !inString) {
                String stmt = current.toString().trim();
                if (!stmt.isBlank()) {
                    statements.add(stmt);
                }
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        // Capture final statement without trailing semicolon
        String last = current.toString().trim();
        if (!last.isBlank()) {
            statements.add(last);
        }

        return statements;
    }
}
