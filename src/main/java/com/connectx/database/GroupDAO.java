package com.connectx.database;

import java.sql.*;
import java.util.*;

public class GroupDAO {
    public static int createGroup(String name, String creator, List<String> members) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty() || creator == null || creator.isBlank()) return -1;
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            int id;
            try (PreparedStatement s = c.prepareStatement(
                    "INSERT INTO chat_groups(group_name,created_by) VALUES(?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, clean); s.setString(2, creator); s.executeUpdate();
                try (ResultSet r = s.getGeneratedKeys()) {
                    if (!r.next()) { c.rollback(); return -1; }
                    id = r.getInt(1);
                }
            }
            LinkedHashSet<String> all = new LinkedHashSet<>();
            all.add(creator);
            if (members != null) all.addAll(members);
            try (PreparedStatement s = c.prepareStatement(
                    "INSERT OR IGNORE INTO group_members(group_id,username) VALUES(?,?)")) {
                for (String member : all) {
                    if (member != null && !member.isBlank()) {
                        s.setInt(1,id); s.setString(2,member.trim()); s.addBatch();
                    }
                }
                s.executeBatch();
            }
            c.commit();
            return id;
        } catch (Exception e) { System.out.println("Create group error: " + e.getMessage()); return -1; }
    }

    public static List<String> getUserGroups(String username) {
        List<String> groups = new ArrayList<>();
        String sql = """
            SELECT g.id || '|' || g.group_name FROM chat_groups g
            JOIN group_members m ON m.group_id=g.id WHERE m.username=? ORDER BY g.group_name
        """;
        try (Connection c = Database.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1,username);
            try (ResultSet r = s.executeQuery()) { while (r.next()) groups.add(r.getString(1)); }
        } catch (Exception e) { System.out.println("Load groups error: " + e.getMessage()); }
        return groups;
    }

    public static boolean isMember(int groupId, String username) {
        try (Connection c = Database.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT 1 FROM group_members WHERE group_id=? AND username=?")) {
            s.setInt(1,groupId); s.setString(2,username);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        } catch (Exception e) { return false; }
    }

    public static boolean saveGroupMessage(int groupId, String sender, String message) {
        if (!isMember(groupId,sender)) return false;
        try (Connection c = Database.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO group_messages(group_id,sender,message) VALUES(?,?,?)")) {
            s.setInt(1,groupId); s.setString(2,sender); s.setString(3,message);
            return s.executeUpdate() > 0;
        } catch (Exception e) { System.out.println("Group message error: " + e.getMessage()); return false; }
    }

    public static List<String> getGroupHistory(int groupId, String username) {
        List<String> result = new ArrayList<>();
        if (!isMember(groupId,username)) return result;
        try (Connection c = Database.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT sender,message,sent_at FROM group_messages WHERE group_id=? ORDER BY id")) {
            s.setInt(1,groupId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) result.add(r.getString("sender") + ": " + r.getString("message") +
                        " (" + r.getString("sent_at") + ")");
            }
        } catch (Exception e) { System.out.println("Group history error: " + e.getMessage()); }
        return result;
    }
}
