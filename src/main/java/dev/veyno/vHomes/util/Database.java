package dev.veyno.vHomes.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Database {

    public enum DatabaseType {
        MYSQL, SQLITE
    }

    private final DatabaseType dbType;
    private final String tableName;
    private HikariDataSource dataSource;
    private final Gson gson = new Gson();
    private final int valueColumns;

    private final String connectionUrl;


    public Database(String tableName, String jdbcUrl, int valueColumns) {
        if (valueColumns < 1) {
            throw new IllegalArgumentException("Es muss mindestens eine Value-Spalte geben");
        }

        this.dbType = DatabaseType.MYSQL;
        this.connectionUrl = jdbcUrl;
        this.tableName = tableName;
        this.valueColumns = valueColumns;

        initializePool();
    }


    public Database(String tableName, int valueColumns, String relativePath, String dbFileName) {
        if (valueColumns < 1) {
            throw new IllegalArgumentException("Es muss mindestens eine Value-Spalte geben");
        }

        this.dbType = DatabaseType.SQLITE;
        this.tableName = tableName;
        this.valueColumns = valueColumns;

        Path fullPath;
        if (relativePath != null && !relativePath.isEmpty()) {
            fullPath = Paths.get(relativePath, dbFileName);
        } else {
            fullPath = Paths.get(dbFileName);
        }

        File dbFile = fullPath.toFile();
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        this.connectionUrl = "jdbc:sqlite:" + fullPath.toString();
        initializePool();
    }

    public Database(String tableName, int valueColumns, String dbFileName) {
        this(tableName, valueColumns, "", dbFileName);
    }


    public Database(String tableName, String dbFileName) {
        this(tableName, 1, "", dbFileName);
    }


    private void initializePool() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(connectionUrl);

            if (dbType == DatabaseType.MYSQL) {
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setConnectionTimeout(5000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                config.setConnectionTestQuery("SELECT 1");
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("autoReconnect", "true");
            }

            dataSource = new HikariDataSource(config);
            createTableIfNotExists();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler bei der Initialisierung des Connection Pools", e);
        }
    }

    private void createTableIfNotExists() throws SQLException {
        StringBuilder createTableSQL = new StringBuilder();

        if (dbType == DatabaseType.MYSQL) {
            createTableSQL.append(String.format("CREATE TABLE IF NOT EXISTS %s (k VARCHAR(255) PRIMARY KEY", tableName));
            for (int i = 0; i < valueColumns; i++) {
                createTableSQL.append(String.format(", v%d TEXT", i));
                createTableSQL.append(String.format(", data_type%d VARCHAR(50)", i));
            }
            createTableSQL.append(", updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
        } else {
            createTableSQL.append(String.format("CREATE TABLE IF NOT EXISTS %s (k TEXT PRIMARY KEY", tableName));
            for (int i = 0; i < valueColumns; i++) {
                createTableSQL.append(String.format(", v%d TEXT", i));
                createTableSQL.append(String.format(", data_type%d TEXT", i));
            }
            createTableSQL.append(", updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL.toString());
        }
    }

    private void validateColumn(int column) {
        if (column < 0 || column >= valueColumns) {
            throw new IllegalArgumentException(String.format("Ungültige Spalte %d. Erlaubt sind 0-%d", column, valueColumns - 1));
        }
    }

    public void set(String key, int column, String value) {
        validateColumn(column);

        try {
            String upsertSQL;
            if (dbType == DatabaseType.MYSQL) {
                upsertSQL = String.format("INSERT INTO %s (k, v%d, data_type%d) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE v%d = VALUES(v%d), data_type%d = VALUES(data_type%d)",
                        tableName, column, column, column, column, column, column);
            } else {
                upsertSQL = String.format("INSERT INTO %s (k, v%d, data_type%d) VALUES (?, ?, ?) " +
                                "ON CONFLICT(k) DO UPDATE SET v%d = excluded.v%d, data_type%d = excluded.data_type%d",
                        tableName, column, column, column, column, column, column);
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(upsertSQL)) {
                stmt.setString(1, key);
                stmt.setString(2, value);
                stmt.setString(3, "STRING");
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern des Wertes", e);
        }
    }

    public void set(String key, int column, List<String> value) {
        validateColumn(column);

        try {
            String jsonValue = gson.toJson(value);
            String upsertSQL;

            if (dbType == DatabaseType.MYSQL) {
                upsertSQL = String.format(
                        "INSERT INTO %s (k, v%d, data_type%d) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE v%d = VALUES(v%d), data_type%d = VALUES(data_type%d)",
                        tableName, column, column, column, column, column, column);
            } else {
                upsertSQL = String.format(
                        "INSERT INTO %s (k, v%d, data_type%d) VALUES (?, ?, ?) " +
                                "ON CONFLICT(k) DO UPDATE SET v%d = excluded.v%d, data_type%d = excluded.data_type%d",
                        tableName, column, column, column, column, column, column);
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(upsertSQL)) {
                stmt.setString(1, key);
                stmt.setString(2, jsonValue);
                stmt.setString(3, "LIST");
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern der Liste", e);
        }
    }


    public String getString(String key, int column) {
        validateColumn(column);

        try {
            String selectSQL = String.format(
                    "SELECT v%d, data_type%d FROM %s WHERE k = ?",
                    column, column, tableName);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, key);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String value = rs.getString(1);
                        String dataType = rs.getString(2);

                        if ("STRING".equals(dataType)) {
                            return value;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Abrufen des Wertes", e);
        }

        return null;
    }

    public List<String> getList(String key, int column) {
        validateColumn(column);

        try {
            String selectSQL = String.format(
                    "SELECT v%d, data_type%d FROM %s WHERE k = ?",
                    column, column, tableName);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, key);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String jsonValue = rs.getString(1);
                        String dataType = rs.getString(2);

                        if ("LIST".equals(dataType) && jsonValue != null) {
                            return gson.fromJson(jsonValue,
                                    new TypeToken<List<String>>(){}.getType());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Abrufen der Liste", e);
        }

        return null;
    }


    public boolean containsKey(String key) {
        try {
            String selectSQL = String.format("SELECT COUNT(*) FROM %s WHERE k = ?", tableName);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, key);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Prüfen des Schlüssels", e);
        }

        return false;
    }

    public void remove(String key) {
        try {
            String deleteSQL = String.format("DELETE FROM %s WHERE k = ?", tableName);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                pstmt.setString(1, key);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Entfernen des Wertes", e);
        }
    }

    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();

        try {
            String selectSQL = String.format("SELECT k FROM %s", tableName);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSQL)) {

                while (rs.next()) {
                    keys.add(rs.getString("k"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Abrufen der Schlüssel", e);
        }

        return keys;
    }

    public int size() {
        try {
            String countSQL = String.format("SELECT COUNT(*) FROM %s", tableName);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSQL)) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Zählen der Einträge", e);
        }

        return 0;
    }

    public void clear() {
        try {
            String deleteSQL = String.format("DELETE FROM %s", tableName);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(deleteSQL);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Leeren der Tabelle", e);
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public int getValueColumnCount() {
        return valueColumns;
    }

    @Deprecated
    public void set(String key, String value) {
        set(key, 0, value);
    }

    @Deprecated
    public void set(String key, List<String> value) {
        set(key, 0, value);
    }

    @Deprecated
    public String getString(String key) {
        return getString(key, 0);
    }

    @Deprecated
    public List<String> getList(String key) {
        return getList(key, 0);
    }
}