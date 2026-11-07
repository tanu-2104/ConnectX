package com.connectx.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.File;

public class Database {
    private static final String URL = "jdbc:sqlite:data/connectx.db";

    public static Connection getConnection() throws Exception {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 10000");
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initializeDatabase() {
        try {
            File folder = new File("data");
            if (!folder.exists()) folder.mkdirs();

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA synchronous = NORMAL");

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        email TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sender TEXT NOT NULL,
                        receiver TEXT,
                        message TEXT NOT NULL,
                        sent_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                addColumnIfMissing(connection, "users", "display_name", "TEXT");
                addColumnIfMissing(connection, "users", "bio", "TEXT");
                addColumnIfMissing(connection, "users", "profile_picture", "TEXT");
                addColumnIfMissing(connection, "messages", "status",
                        "TEXT NOT NULL DEFAULT 'SENT'");

                // Group chat support. Existing private-message rows are untouched.
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS chat_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_name TEXT NOT NULL,
                        created_by TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS group_members (
                        group_id INTEGER NOT NULL,
                        username TEXT NOT NULL,
                        joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY(group_id, username),
                        FOREIGN KEY(group_id) REFERENCES chat_groups(id) ON DELETE CASCADE
                    )
                """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS group_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_id INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        message TEXT NOT NULL,
                        sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(group_id) REFERENCES chat_groups(id) ON DELETE CASCADE
                    )
                """);

                // Reactions and attachments are stored separately from message text.
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS message_reactions (
                        message_id INTEGER NOT NULL,
                        username TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY(message_id, username, emoji),
                        FOREIGN KEY(message_id) REFERENCES messages(id) ON DELETE CASCADE
                    )
                """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS message_attachments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        message_id INTEGER NOT NULL,
                        original_name TEXT NOT NULL,
                        stored_path TEXT NOT NULL,
                        mime_type TEXT,
                        file_size INTEGER NOT NULL,
                        FOREIGN KEY(message_id) REFERENCES messages(id) ON DELETE CASCADE
                    )
                """);

                // Unread counts are per recipient and private conversation.
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS unread_counts (
                        username TEXT NOT NULL,
                        conversation_key TEXT NOT NULL,
                        unread_count INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(username, conversation_key)
                    )
                """);

                System.out.println("ConnectX database and feature tables are ready.");
            }
        } catch (Exception e) {
            System.out.println("Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addColumnIfMissing(Connection connection, String tableName,
                                           String columnName, String columnType) throws Exception {
        boolean exists = false;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " +
                        columnName + " " + columnType);
            }
        }
    }

    public static void main(String[] args) {
        initializeDatabase();
    }
}
