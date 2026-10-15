package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class V1__RepairLegacyAuditTimestamps extends BaseJavaMigration {

    private static final List<String> AUDITED_TABLES = List.of(
            "users",
            "student_profiles",
            "recruiter_profiles",
            "companies",
            "jobs",
            "job_applications",
            "interviews",
            "placement_drives"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql")) {
            return;
        }

        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        Map<String, String> tables = findTables(metadata, catalog);
        try (Statement statement = connection.createStatement()) {
            for (String expectedTable : AUDITED_TABLES) {
                String table = tables.get(expectedTable);
                if (table == null) {
                    continue;
                }
                ensureAuditColumns(metadata, catalog, table, statement);
            }
        }
    }

    private Map<String, String> findTables(DatabaseMetaData metadata, String catalog) throws Exception {
        Map<String, String> tables = new HashMap<>();
        try (ResultSet results = metadata.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (results.next()) {
                String table = results.getString("TABLE_NAME");
                tables.put(table.toLowerCase(Locale.ROOT), table);
            }
        }
        return tables;
    }

    private void ensureAuditColumns(
            DatabaseMetaData metadata,
            String catalog,
            String table,
            Statement statement
    ) throws Exception {
        Map<String, String> columns = new HashMap<>();
        try (ResultSet results = metadata.getColumns(catalog, null, table, "%")) {
            while (results.next()) {
                String column = results.getString("COLUMN_NAME");
                columns.put(column.toLowerCase(Locale.ROOT), column);
            }
        }

        List<String> newlyAdded = new ArrayList<>();
        addColumnIfMissing(table, columns, "created_at", newlyAdded, statement);
        addColumnIfMissing(table, columns, "updated_at", newlyAdded, statement);
        if (newlyAdded.isEmpty()) {
            return;
        }

        String assignments = newlyAdded.stream()
                .map(column -> "`" + column + "` = CURRENT_TIMESTAMP(6)")
                .collect(java.util.stream.Collectors.joining(", "));
        statement.executeUpdate("UPDATE `" + table + "` SET " + assignments);
        for (String column : newlyAdded) {
            statement.execute("ALTER TABLE `" + table + "` MODIFY COLUMN `" + column
                    + "` DATETIME(6) NOT NULL");
        }
    }

    private void addColumnIfMissing(
            String table,
            Map<String, String> columns,
            String column,
            List<String> newlyAdded,
            Statement statement
    ) throws Exception {
        if (columns.containsKey(column)) {
            return;
        }
        statement.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` DATETIME(6) NULL");
        newlyAdded.add(column);
    }
}
