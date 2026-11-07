
package com.connectx.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.connectx.utils.PasswordUtil;

public class UserDAO {

    // Register a new user
    public static String registerUser(
            String username,
            String email,
            String password) {

        String sql = """
            INSERT INTO users (username, email, password_hash)
            VALUES (?, ?, ?)
            """;

        try {
            String passwordHash =
                PasswordUtil.hashPassword(password);

            try (Connection connection = Database.getConnection();
                 PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setString(1, username);
                statement.setString(2, email);
                statement.setString(3, passwordHash);

                statement.executeUpdate();

                return "SUCCESS";
            }

        } catch (java.sql.SQLException e) {

            if (e.getMessage().toLowerCase().contains("unique")) {
                return "Username or email already exists.";
            }

            e.printStackTrace();
            return "Database error. Please try again.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Could not securely process the password.";
        }
    }

    // Login an existing user
    public static boolean loginUser(
            String username,
            String password) {

        String sql = """
            SELECT password_hash
            FROM users
            WHERE username = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    String storedHash =
                        result.getString("password_hash");

                    return PasswordUtil.verifyPassword(
                        password,
                        storedHash
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Check whether a username exists
    public static boolean userExists(String username) {

        String sql = """
            SELECT id
            FROM users
            WHERE username = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }

        } catch (Exception e) {
            System.out.println(
                "Error checking username: " + e.getMessage()
            );
            return false;
        }
    }

    // Update profile details
    public static boolean updateProfile(
            String username,
            String displayName,
            String bio,
            String profilePicture) {

        String sql = """
            UPDATE users
            SET display_name = ?,
                bio = ?,
                profile_picture = ?
            WHERE username = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, displayName);
            statement.setString(2, bio);
            statement.setString(3, profilePicture);
            statement.setString(4, username);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println(
                "Error updating profile: " + e.getMessage()
            );
            return false;
        }
    }

    // Get profile details
    public static UserProfile getUserProfile(String username) {

        String sql = """
            SELECT username, email, display_name, bio, profile_picture
            FROM users
            WHERE username = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    return new UserProfile(
                        result.getString("username"),
                        result.getString("email"),
                        result.getString("display_name"),
                        result.getString("bio"),
                        result.getString("profile_picture")
                    );
                }
            }

        } catch (Exception e) {
            System.out.println(
                "Error loading profile: " + e.getMessage()
            );
        }

        return null;
    }

    // Store profile information
    public static class UserProfile {

        private final String username;
        private final String email;
        private final String displayName;
        private final String bio;
        private final String profilePicture;

        public UserProfile(
                String username,
                String email,
                String displayName,
                String bio,
                String profilePicture) {

            this.username = username;
            this.email = email;
            this.displayName = displayName;
            this.bio = bio;
            this.profilePicture = profilePicture;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBio() {
            return bio;
        }

        public String getProfilePicture() {
            return profilePicture;
        }
    }
}
