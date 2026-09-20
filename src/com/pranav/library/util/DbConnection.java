package com.pranav.library.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single place that knows how to open a JDBC connection.
 * Reads config from environment variables so the DB URL/credentials never
 * need to be hardcoded or committed to git.
 *
 * Requires the MySQL Connector/J jar on the runtime classpath
 * (see backend/lib/README.txt) — java.sql.* itself is part of the JDK,
 * so this class compiles with no external dependency at all.
 */
public final class DbConnection {

    private static final String URL =
            System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/library_db");
    private static final String USER =
            System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD =
            System.getenv().getOrDefault("DB_PASSWORD", "");

    private DbConnection() { }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
