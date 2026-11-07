package com.connectx.database;

import java.sql.*;

public class AttachmentDAO {
    public static boolean saveAttachment(int messageId, String originalName,
                                         String storedPath, String mimeType, long size) {
        String sql = """
            INSERT INTO message_attachments(message_id,original_name,stored_path,mime_type,file_size)
            VALUES(?,?,?,?,?)
        """;
        try (Connection c = Database.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1,messageId); s.setString(2,originalName); s.setString(3,storedPath);
            s.setString(4,mimeType); s.setLong(5,size);
            return s.executeUpdate() > 0;
        } catch (Exception e) { System.out.println("Attachment save error: " + e.getMessage()); return false; }
    }

    public static String[] getAttachment(int messageId) {
        try (Connection c = Database.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT original_name,stored_path,mime_type,file_size FROM message_attachments WHERE message_id=? LIMIT 1")) {
            s.setInt(1,messageId);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) return new String[]{r.getString(1),r.getString(2),r.getString(3),String.valueOf(r.getLong(4))};
            }
        } catch (Exception e) { System.out.println("Attachment read error: " + e.getMessage()); }
        return null;
    }
}
