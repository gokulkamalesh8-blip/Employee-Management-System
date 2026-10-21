package com.example.ems.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Reads settings in this order: environment variables, then db.properties
 * in the project folder. Keys: DB_URL (optional), DB_USER, DB_PASSWORD.
 */
public final class DBConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3307/employee_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final Properties FILE = loadFile();

    private DBConnection() {}

    private static Properties loadFile() {
        Properties p = new Properties();
        Path path = Path.of("db.properties");
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read db.properties", e);
            }
        }
        return p;
    }

    private static String setting(String key) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : FILE.getProperty(key);
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("jdbc:mysql://localhost:3306/employee_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = "root";
        String password = "your_password";
        if (user == null || password == null) {
            throw new IllegalStateException(
                    "Settings not found. Looking for: " + Path.of("db.properties").toAbsolutePath()
                            + " (needs DB_USER and DB_PASSWORD)");        }
        return DriverManager.getConnection(url != null ? url : DEFAULT_URL, user, password);
    }
}