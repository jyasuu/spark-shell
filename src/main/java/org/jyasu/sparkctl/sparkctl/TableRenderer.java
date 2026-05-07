package org.jyasu.sparkctl.sparkctl;

import java.util.List;

public class TableRenderer {

    public static String render(QueryResult result) {
        if (!result.isResultSet()) {
            return String.format("OK. Rows affected: %d  (%d ms)",
                    result.getUpdateCount(), result.getElapsedMs());
        }

        List<String> cols = result.getColumns();
        List<List<String>> rows = result.getRows();

        if (cols.isEmpty()) {
            return "(no columns returned)";
        }

        // Compute column widths
        int[] widths = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            widths[i] = cols.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < row.size() && i < widths.length; i++) {
                widths[i] = Math.max(widths[i], row.get(i).length());
            }
        }

        StringBuilder sb = new StringBuilder();
        String separator = buildSeparator(widths);

        sb.append(separator).append("\n");
        sb.append(buildRow(cols, widths)).append("\n");
        sb.append(separator).append("\n");

        for (List<String> row : rows) {
            sb.append(buildRow(row, widths)).append("\n");
        }
        sb.append(separator).append("\n");
        sb.append(String.format("%d row(s) in set  (%d ms)%n",
                result.getRowCount(), result.getElapsedMs()));

        return sb.toString();
    }

    private static String buildSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    private static String buildRow(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = i < cells.size() ? cells.get(i) : "";
            sb.append(String.format(" %-" + widths[i] + "s |", cell));
        }
        return sb.toString();
    }
}
