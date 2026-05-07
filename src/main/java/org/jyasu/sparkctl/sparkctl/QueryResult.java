package org.jyasu.sparkctl.sparkctl;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QueryResult {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final int updateCount;
    private final boolean isResultSet;
    private final long elapsedMs;

    private QueryResult(List<String> columns, List<List<String>> rows, int updateCount,
                        boolean isResultSet, long elapsedMs) {
        this.columns = columns;
        this.rows = rows;
        this.updateCount = updateCount;
        this.isResultSet = isResultSet;
        this.elapsedMs = elapsedMs;
    }

    public static QueryResult fromResultSet(ResultSet rs, long elapsedMs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }

        List<List<String>> rows = new ArrayList<>();
        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                Object val = rs.getObject(i);
                row.add(val == null ? "NULL" : val.toString());
            }
            rows.add(row);
        }
        return new QueryResult(columns, rows, -1, true, elapsedMs);
    }

    public static QueryResult fromUpdateCount(int updateCount, long elapsedMs) {
        return new QueryResult(List.of(), List.of(), updateCount, false, elapsedMs);
    }

    public List<String> getColumns() { return columns; }
    public List<List<String>> getRows() { return rows; }
    public int getUpdateCount() { return updateCount; }
    public boolean isResultSet() { return isResultSet; }
    public long getElapsedMs() { return elapsedMs; }
    public int getRowCount() { return rows.size(); }
}
