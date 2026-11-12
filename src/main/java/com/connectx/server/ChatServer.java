
package com.connectx.server;

import com.connectx.database.Database;
import com.connectx.database.MessageDAO;
import com.connectx.database.UserDAO;
import com.connectx.database.AttachmentDAO;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private static final int PORT = 5050;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Path ATTACHMENT_FOLDER =
            Paths.get("attachments");

    private static final ConcurrentHashMap<String, PrintWriter> clients =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        System.out.println("Starting ConnectX Chat Server...");

        Database.initializeDatabase();

        try {
            Files.createDirectories(ATTACHMENT_FOLDER);
        } catch (IOException e) {
            System.out.println("Attachment folder error: " + e.getMessage());
            return;
        }

        try (javax.net.ssl.SSLServerSocket serverSocket = (javax.net.ssl.SSLServerSocket) com.connectx.security.TlsConfig.createServerContext().getServerSocketFactory().createServerSocket(PORT)) {
            serverSocket.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});

            System.out.println("ConnectX server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }

        } catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }

    private static String encode(String value) {
        if (value == null) value = "";

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(
                    Base64.getDecoder().decode(value),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            return new String(
                    Base64.getUrlDecoder().decode(value),
                    StandardCharsets.UTF_8
            );
        }
    }

    private static void broadcastUserList() {

        StringBuilder userList = new StringBuilder();

        for (String username : clients.keySet()) {

            UserDAO.UserProfile profile =
                    UserDAO.getUserProfile(username);

            String displayName = username;
            String picturePath = "";

            if (profile != null) {
                if (profile.getDisplayName() != null &&
                        !profile.getDisplayName().isBlank()) {
                    displayName = profile.getDisplayName();
                }

                picturePath = profile.getProfilePicture();
            }

            if (userList.length() > 0) {
                userList.append(",");
            }

            userList.append(encode(username))
                    .append(":")
                    .append(encode(displayName))
                    .append(":")
                    .append(encode(picturePath));
        }

        String result = "USERLIST|" + userList;

        for (PrintWriter client : clients.values()) {
            client.println(result);
        }
    }

    private static void sendToUser(String username, String message) {
        PrintWriter client = clients.get(username);

        if (client != null) {
            client.println(message);
        }
    }

    private static void markMessagesAsSeen(
            String sender, String receiver) {

        String sql = """
            SELECT id FROM messages
            WHERE sender = ? AND receiver = ?
            AND status != 'SEEN'
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");

                    if (MessageDAO.updateMessageStatus(id, "SEEN")) {
                        sendToUser(sender, "STATUS|" + id + "|SEEN");
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Seen update error: " + e.getMessage());
        }
    }

    private static String safeFileName(String name) {

        if (name == null || name.isBlank()) {
            return "attachment";
        }

        String cleaned = Paths.get(name)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        return cleaned.isBlank() ? "attachment" : cleaned;
    }

    private static boolean allowedFile(String name) {

        String lower = name.toLowerCase();

        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.endsWith(".pdf")
                || lower.endsWith(".txt")
                || lower.endsWith(".doc")
                || lower.endsWith(".docx")
                || lower.endsWith(".xls")
                || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt")
                || lower.endsWith(".pptx");
    }

    private static class ClientHandler implements Runnable {

        private final Socket socket;
        private String username;
        private PrintWriter writer;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            try {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

                writer = new PrintWriter(
                        socket.getOutputStream(),
                        true,
                        StandardCharsets.UTF_8
                );

                String login = reader.readLine();

                if (login == null) {
                    return;
                }

                String[] authParts = login.split("\\|", -1);

                if ("REGISTER".equals(authParts[0])) {

                    if (authParts.length != 4) {
                        writer.println("REGISTER_FAILED|" +
                                encode("Invalid registration request."));
                        return;
                    }

                    String newUsername = decode(authParts[1]).trim();
                    String email = decode(authParts[2]).trim();
                    String password = decode(authParts[3]);

                    if (newUsername.isEmpty() || email.isEmpty()
                            || password.length() < 8) {
                        writer.println("REGISTER_FAILED|" +
                                encode("Please provide valid registration details."));
                        return;
                    }

                    String result = UserDAO.registerUser(
                            newUsername, email, password
                    );

                    if ("SUCCESS".equals(result)) {
                        writer.println("REGISTER_OK");
                    } else {
                        writer.println("REGISTER_FAILED|" + encode(result));
                    }

                    return;
                }

                if (!"LOGIN".equals(authParts[0])
                        || authParts.length != 3) {
                    writer.println("AUTH_FAILED|" +
                            encode("Invalid login request."));
                    return;
                }

                String requestedUsername = decode(authParts[1]).trim();
                String password = decode(authParts[2]);

                if (requestedUsername.isEmpty() || password.isEmpty()
                        || !UserDAO.loginUser(requestedUsername, password)) {
                    writer.println("AUTH_FAILED|" +
                            encode("Invalid username or password."));
                    return;
                }

                PrintWriter existing =
                        clients.putIfAbsent(requestedUsername, writer);

                if (existing != null) {
                    writer.println("AUTH_FAILED|" +
                            encode("This user is already online."));
                    return;
                }

                username = requestedUsername;

                writer.println("AUTH_OK");

                System.out.println(username + " connected.");

                writer.println("SYSTEM|Connected as " + username);
                broadcastUserList();
                String message;

                while ((message = reader.readLine()) != null) {

                    String[] parts = message.split("\\|", -1);

                    switch (parts[0]) {
                        case "HISTORY":

                            if (parts.length != 2) {
                                writer.println("ERROR|Invalid history request.");
                                break;
                            }

                            String partner = decode(parts[1]);

                            if (!UserDAO.userExists(partner)) {
                                writer.println("ERROR|User not found.");
                                break;
                            }

                            try {
                                java.util.List<MessageDAO.ChatMessage> history =
                                        MessageDAO.getPrivateChatHistoryRecords(
                                                username, partner);

                                for (MessageDAO.ChatMessage record : history) {
                                    String attachmentData = "";

                                    if (record.attachmentData != null) {
                                        attachmentData = Base64.getEncoder()
                                                .encodeToString(record.attachmentData);
                                    }

                                    writer.println(
                                            "HISTORY_ITEM|" +
                                            record.id + "|" +
                                            encode(record.sender) + "|" +
                                            encode(record.message) + "|" +
                                            encode(record.sentAt) + "|" +
                                            encode(record.attachmentName) + "|" +
                                            encode(record.mimeType) + "|" +
                                            attachmentData
                                    );
                                }

                                writer.println("HISTORY_DONE");

                            } catch (Exception e) {
                                writer.println("ERROR|Could not load chat history.");
                                System.out.println("History error: " + e.getMessage());
                            }

                            break;


                        case "PROFILE_UPDATE":

                            if (parts.length == 2 &&
                                    decode(parts[1]).equals(username)) {
                                broadcastUserList();
                            }

                            break;

                        case "PRIVATE":

                            if (parts.length != 4) {
                                writer.println("ERROR|Invalid private message.");
                                break;
                            }

                            String sender = decode(parts[1]);
                            String receiver = decode(parts[2]);
                            String text = decode(parts[3]);

                            if (!sender.equals(username)) {
                                writer.println("ERROR|Invalid sender.");
                                break;
                            }

                            int messageId =
                                    MessageDAO.saveMessageAndGetId(
                                            sender, receiver, text
                                    );

                            if (messageId == -1) {
                                writer.println("ERROR|Message could not be saved.");
                                break;
                            }

                            writer.println(
                                    "SENT|" + messageId + "|" +
                                            sender + "|" + receiver
                            );

                            if (clients.containsKey(receiver)) {
                                sendToUser(
                                        receiver,
                                        "PRIVATE|" + messageId + "|" +
                                                sender + "|" + receiver + "|" +
                                                text
                                );
                            } else {
                                writer.println("OFFLINE|" + receiver);
                            }

                            break;

                        case "TYPING":

                            if (parts.length == 3) {
                                String typingSender = decode(parts[1]);
                                String typingReceiver = decode(parts[2]);

                                if (typingSender.equals(username)) {
                                    sendToUser(
                                            typingReceiver,
                                            "TYPING|" + username
                                    );
                                }
                            }

                            break;

                        case "STOP_TYPING":

                            if (parts.length == 3) {
                                String typingSender = decode(parts[1]);
                                String typingReceiver = decode(parts[2]);

                                if (typingSender.equals(username)) {
                                    sendToUser(
                                            typingReceiver,
                                            "STOP_TYPING|" + username
                                    );
                                }
                            }

                            break;

                        case "SEARCH":

                            if (parts.length == 4) {

                                String searchUser = decode(parts[1]);
                                String contact = decode(parts[2]);
                                String query = decode(parts[3]);

                                if (!searchUser.equals(username)) {
                                    writer.println("ERROR|Invalid search user.");
                                    break;
                                }

                                searchMessages(contact, username, query);
                            }

                            break;

                        // FILE SHARING
                        case "FILE":

                            receiveFile(parts);

                            break;

                        case "DELIVERED":

                            if (parts.length == 2) {
                                try {
                                    int id = Integer.parseInt(parts[1]);

                                    String sql = """
                                        SELECT sender FROM messages
                                        WHERE id = ? AND receiver = ?
                                        """;

                                    try (Connection conn = Database.getConnection();
                                         PreparedStatement stmt =
                                                 conn.prepareStatement(sql)) {

                                        stmt.setInt(1, id);
                                        stmt.setString(2, username);

                                        try (ResultSet rs = stmt.executeQuery()) {
                                            if (rs.next()) {
                                                String originalSender =
                                                        rs.getString("sender");

                                                MessageDAO.updateMessageStatus(
                                                        id, "DELIVERED"
                                                );

                                                sendToUser(
                                                        originalSender,
                                                        "STATUS|" + id + "|DELIVERED"
                                                );
                                            }
                                        }
                                    }

                                } catch (NumberFormatException e) {
                                    writer.println("ERROR|Invalid message ID.");
                                }
                            }

                            break;

                        case "SEEN":

                            if (parts.length == 2) {
                                String contact = decode(parts[1]);

                                if (!contact.equals(username)) {
                                    markMessagesAsSeen(contact, username);
                                }
                            }

                            break;

                        default:
                            writer.println(
                                    "ERROR|Unsupported command: " + parts[0]
                            );
                    }
                }

            } catch (Exception e) {
                System.out.println("Client error: " + e.getMessage());

            } finally {

                if (username != null && writer != null) {

                    if (clients.remove(username, writer)) {
                        System.out.println(username + " disconnected.");
                        broadcastUserList();
                    }
                }

                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }

        private void receiveFile(String[] parts) {

            if (parts.length != 6) {
                writer.println("ERROR|Invalid attachment data.");
                return;
            }

            String sender = decode(parts[1]);
            String receiver = decode(parts[2]);
            String originalName = decode(parts[3]);
            String mimeType = decode(parts[4]);
            String fileData = parts[5];

            if (!sender.equals(username)) {
                writer.println("ERROR|Invalid attachment sender.");
                return;
            }

            if (!UserDAO.userExists(receiver)) {
                writer.println("ERROR|Recipient does not exist.");
                return;
            }

            originalName = safeFileName(originalName);

            if (!allowedFile(originalName)) {
                writer.println("ERROR|This file type is not supported.");
                return;
            }

            try {

                byte[] bytes = Base64.getDecoder().decode(fileData);

                if (bytes.length == 0) {
                    writer.println("ERROR|The selected file is empty.");
                    return;
                }

                if (bytes.length > MAX_FILE_SIZE) {
                    writer.println("ERROR|File size must be 5 MB or less.");
                    return;
                }

                int messageId = MessageDAO.saveMessageAndGetId(
                        sender,
                        receiver,
                        "[Attachment] " + originalName
                );

                if (messageId == -1) {
                    writer.println("ERROR|Could not save attachment message.");
                    return;
                }

                String storedName =
                        messageId + "_" + originalName;

                Path destination =
                        ATTACHMENT_FOLDER.resolve(storedName);

                Files.write(destination, bytes);

                boolean saved = AttachmentDAO.saveAttachment(
                        messageId,
                        originalName,
                        destination.toString(),
                        mimeType,
                        bytes.length
                );

                if (!saved) {
                    Files.deleteIfExists(destination);
                    writer.println("ERROR|Could not save attachment details.");
                    return;
                }

                String response =
                        "FILE_RECEIVED|" +
                        messageId + "|" +
                        sender + "|" +
                        receiver + "|" +
                        encode(originalName) + "|" +
                        encode(mimeType) + "|" +
                        fileData;

                if (clients.containsKey(receiver)) {
                    sendToUser(receiver, response);
                } else {
                    writer.println("OFFLINE|" + receiver);
                }

                writer.println("FILE_SENT|" + messageId);

                System.out.println(
                        "Attachment received from " + sender +
                        ": " + originalName +
                        " (" + bytes.length + " bytes)"
                );

            } catch (IllegalArgumentException e) {
                writer.println("ERROR|Invalid file data.");

            } catch (Exception e) {
                writer.println("ERROR|File transfer failed.");
                System.out.println("File transfer error: " + e.getMessage());
            }
        }

        private void searchMessages(
                String contact, String currentUser, String query) {

            String sql = """
                SELECT id, sender, receiver, message, sent_at
                FROM messages
                WHERE (
                    (sender = ? AND receiver = ?)
                    OR
                    (sender = ? AND receiver = ?)
                )
                AND message LIKE ?
                ORDER BY id DESC
                LIMIT 100
                """;

            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, currentUser);
                stmt.setString(2, contact);
                stmt.setString(3, contact);
                stmt.setString(4, currentUser);
                stmt.setString(5, "%" + query + "%");

                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {

                        String result =
                                "SEARCH_RESULT|" +
                                rs.getInt("id") + "|" +
                                encode(rs.getString("sender")) + "|" +
                                encode(rs.getString("receiver")) + "|" +
                                encode(rs.getString("message")) + "|" +
                                encode(rs.getString("sent_at"));

                        writer.println(result);
                    }
                }

                writer.println("SEARCH_DONE");

            } catch (Exception e) {
                writer.println("ERROR|Search failed.");
                System.out.println("Search error: " + e.getMessage());
            }
        }
    }
}
