
package com.connectx.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

public class MessageDAO {

    // Represents a message, including its attachment when available.
    public static class ChatMessage {
        public int id;
        public String sender;
        public String message;
        public String sentAt;

        public String attachmentName;
        public String mimeType;
        public byte[] attachmentData;

        public boolean hasAttachment() {
            return attachmentData != null && attachmentData.length > 0;
        }

        public boolean isImage() {
            if (mimeType != null && mimeType.toLowerCase().startsWith("image/")) {
                return true;
            }

            if (attachmentName == null) return false;

            String name = attachmentName.toLowerCase();
            return name.endsWith(".png")
                    || name.endsWith(".jpg")
                    || name.endsWith(".jpeg")
                    || name.endsWith(".gif")
                    || name.endsWith(".webp");
        }
    }

    public static int saveMessageAndGetId(
            String sender, String receiver, String message) {

        String sql = "INSERT INTO messages(sender, receiver, message, status) VALUES(?,?,?,'SENT')";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, message);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (Exception e) {
            System.out.println("Error saving message: " + e.getMessage());
        }

        return -1;
    }

    public static boolean saveMessage(
            String sender, String receiver, String message) {
        return saveMessageAndGetId(sender, receiver, message) != -1;
    }

    public static boolean updateMessageStatus(int messageId, String status) {

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE messages SET status=? WHERE id=?")) {

            stmt.setString(1, status);
            stmt.setInt(2, messageId);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error updating status: " + e.getMessage());
            return false;
        }
    }

    public static List<String> getAllMessages() {

        List<String> result = new ArrayList<>();

        String sql = """
            SELECT sender,message,sent_at
            FROM messages
            WHERE receiver='ALL'
            ORDER BY id
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {

            while (r.next()) {
                result.add(
                        r.getString("sender") + ": "
                        + r.getString("message") + " ("
                        + r.getString("sent_at") + ")"
                );
            }

        } catch (Exception e) {
            System.out.println("Error loading messages: " + e.getMessage());
        }

        return result;
    }

    // Existing history method retained for compatibility.
    public static List<String> getPrivateChatHistory(
            String user1, String user2) {

        List<String> result = new ArrayList<>();

        String sql = """
            SELECT sender,message,sent_at
            FROM messages
            WHERE (sender=? AND receiver=?)
               OR (sender=? AND receiver=?)
            ORDER BY id ASC
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {

            s.setString(1, user1);
            s.setString(2, user2);
            s.setString(3, user2);
            s.setString(4, user1);

            try (ResultSet r = s.executeQuery()) {

                while (r.next()) {
                    result.add(
                            r.getString("sender") + ": "
                            + r.getString("message") + " ("
                            + r.getString("sent_at") + ")"
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("Error loading private history: " + e.getMessage());
        }

        return result;
    }

    // NEW: Loads complete conversation history, including attachment bytes.
    public static List<ChatMessage> getPrivateChatHistoryRecords(
            String user1, String user2) {

        List<ChatMessage> result = new ArrayList<>();

        String sql = """
            SELECT
                m.id,
                m.sender,
                m.message,
                m.sent_at,
                a.original_name,
                a.stored_path,
                a.mime_type
            FROM messages m
            LEFT JOIN message_attachments a
                ON a.message_id = m.id
            WHERE
                (m.sender=? AND m.receiver=?)
                OR
                (m.sender=? AND m.receiver=?)
            ORDER BY m.id ASC
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {

            s.setString(1, user1);
            s.setString(2, user2);
            s.setString(3, user2);
            s.setString(4, user1);

            try (ResultSet r = s.executeQuery()) {

                while (r.next()) {

                    ChatMessage chatMessage = new ChatMessage();

                    chatMessage.id = r.getInt("id");
                    chatMessage.sender = r.getString("sender");
                    chatMessage.message = r.getString("message");
                    chatMessage.sentAt = r.getString("sent_at");

                    chatMessage.attachmentName =
                            r.getString("original_name");

                    chatMessage.mimeType =
                            r.getString("mime_type");

                    String storedPath = r.getString("stored_path");

                    // Load the actual saved attachment from disk.
                    if (storedPath != null && !storedPath.isBlank()) {

                        try {
                            Path filePath = Path.of(storedPath);

                            if (Files.exists(filePath)
                                    && Files.isRegularFile(filePath)) {

                                chatMessage.attachmentData =
                                        Files.readAllBytes(filePath);

                            } else {
                                System.out.println(
                                        "Attachment file not found: " + storedPath
                                );
                            }

                        } catch (Exception fileError) {
                            System.out.println(
                                    "Attachment loading error: "
                                    + fileError.getMessage()
                            );
                        }
                    }

                    result.add(chatMessage);
                }
            }

        } catch (Exception e) {
            System.out.println(
                    "Error loading complete chat history: " + e.getMessage()
            );
        }

        return result;
    }

    public static List<String> searchPrivateMessages(
            String user1, String user2, String query) {

        List<String> result = new ArrayList<>();

        String sql = """
            SELECT sender,message,sent_at
            FROM messages
            WHERE ((sender=? AND receiver=?)
                OR (sender=? AND receiver=?))
              AND message LIKE ? COLLATE NOCASE
            ORDER BY id DESC
            LIMIT 100
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {

            s.setString(1, user1);
            s.setString(2, user2);
            s.setString(3, user2);
            s.setString(4, user1);
            s.setString(5, "%" + query + "%");

            try (ResultSet r = s.executeQuery()) {

                while (r.next()) {
                    result.add(
                            r.getString("sender") + ": "
                            + r.getString("message") + " ("
                            + r.getString("sent_at") + ")"
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
        }

        return result;
    }

    public static boolean addReaction(
            int messageId, String username, String emoji) {

        String sql = """
            INSERT OR IGNORE INTO message_reactions
            (message_id,username,emoji)
            VALUES(?,?,?)
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {

            s.setInt(1, messageId);
            s.setString(2, username);
            s.setString(3, emoji);

            return s.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Reaction error: " + e.getMessage());
            return false;
        }
    }

    public static boolean removeReaction(
            int messageId, String username, String emoji) {

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(
                     """
                     DELETE FROM message_reactions
                     WHERE message_id=? AND username=? AND emoji=?
                     """)) {

            s.setInt(1, messageId);
            s.setString(2, username);
            s.setString(3, emoji);

            return s.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Reaction removal error: " + e.getMessage());
            return false;
        }
    }

    public static List<String> getReactions(int messageId) {

        List<String> result = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(
                     """
                     SELECT username,emoji
                     FROM message_reactions
                     WHERE message_id=?
                     ORDER BY created_at
                     """)) {

            s.setInt(1, messageId);

            try (ResultSet r = s.executeQuery()) {

                while (r.next()) {
                    result.add(
                            r.getString("username") + "|"
                            + r.getString("emoji")
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("Reaction load error: " + e.getMessage());
        }

        return result;
    }

    public static void incrementUnread(
            String username, String conversationKey) {

        String sql = """
            INSERT INTO unread_counts
            (username,conversation_key,unread_count)
            VALUES(?,?,1)
            ON CONFLICT(username,conversation_key)
            DO UPDATE SET unread_count=unread_count+1
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {

            s.setString(1, username);
            s.setString(2, conversationKey);
            s.executeUpdate();

        } catch (Exception e) {
            System.out.println("Unread update error: " + e.getMessage());
        }
    }

    public static int getUnreadCount(
            String username, String conversationKey) {

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(
                     """
                     SELECT unread_count
                     FROM unread_counts
                     WHERE username=? AND conversation_key=?
                     """)) {

            s.setString(1, username);
            s.setString(2, conversationKey);

            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }

        } catch (Exception e) {
            System.out.println("Unread read error: " + e.getMessage());
            return 0;
        }
    }

    public static void clearUnread(
            String username, String conversationKey) {

        try (Connection c = Database.getConnection();
             PreparedStatement s = c.prepareStatement(
                     """
                     DELETE FROM unread_counts
                     WHERE username=? AND conversation_key=?
                     """)) {

            s.setString(1, username);
            s.setString(2, conversationKey);
            s.executeUpdate();

        } catch (Exception e) {
            System.out.println("Unread clear error: " + e.getMessage());
        }
    }
}
