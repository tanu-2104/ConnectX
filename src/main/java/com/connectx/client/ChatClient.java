
package com.connectx.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ChatClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5050;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private MessageListener listener;
    private volatile boolean connected = false;

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public boolean connect(String username) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            writer = new PrintWriter(
                    socket.getOutputStream(), true, StandardCharsets.UTF_8
            );

            reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            connected = true;
            writer.println("LOGIN|" + encode(username));

            Thread receiveThread = new Thread(() -> {
                try {
                    String message;

                    while (connected &&
                            (message = reader.readLine()) != null) {
                        if (listener != null) {
                            listener.onMessageReceived(message);
                        }
                    }
                } catch (Exception e) {
                    if (connected) {
                        System.out.println(
                                "Connection error: " + e.getMessage()
                        );
                    }
                } finally {
                    connected = false;
                }
            });

            receiveThread.setDaemon(true);
            receiveThread.start();

            return true;

        } catch (Exception e) {
            connected = false;
            System.out.println(
                    "Unable to connect to server: " + e.getMessage()
            );
            return false;
        }
    }

    // PRIVATE MESSAGES
    public void sendPrivateMessage(
            String sender, String receiver, String message) {

        send("PRIVATE|" + encode(sender) + "|" +
                encode(receiver) + "|" + encode(message));
    }

    // TYPING INDICATOR
    public void sendTyping(String sender, String receiver) {
        send("TYPING|" + encode(sender) + "|" + encode(receiver));
    }

    public void stopTyping(String sender, String receiver) {
        send("STOP_TYPING|" + encode(sender) + "|" + encode(receiver));
    }

    // MESSAGE SEARCH
    public void searchMessages(String username, String contact, String query) {
        send("SEARCH|" + encode(username) + "|" +
                encode(contact) + "|" + encode(query));
    }

    // EMOJI REACTIONS
    public void sendReaction(
            int messageId, String username, String emoji) {

        send("REACTION|" + messageId + "|" +
                encode(username) + "|" + encode(emoji));
    }

    public void removeReaction(
            int messageId, String username, String emoji) {

        send("REMOVE_REACTION|" + messageId + "|" +
                encode(username) + "|" + encode(emoji));
    }

    // GROUP CHAT
    public void createGroup(String groupName, String creator,
                            String members) {
        send("CREATE_GROUP|" + encode(groupName) + "|" +
                encode(creator) + "|" + encode(members));
    }

    public void sendGroupMessage(
            int groupId, String sender, String message) {

        send("GROUP_MESSAGE|" + groupId + "|" +
                encode(sender) + "|" + encode(message));
    }

    public void requestGroups(String username) {
        send("GET_GROUPS|" + encode(username));
    }

    // FILE SHARING
    public void sendFile(
            String sender, String receiver,
            String fileName, String fileType, String fileData) {

        send("FILE|" + encode(sender) + "|" +
                encode(receiver) + "|" +
                encode(fileName) + "|" +
                encode(fileType) + "|" + fileData);
    }

    // UNREAD MESSAGES
    public void requestUnreadCount(String username) {
        send("GET_UNREAD|" + encode(username));
    }

    public void clearUnread(String username, String sender) {
        send("CLEAR_UNREAD|" + encode(username) + "|" +
                encode(sender));
    }

    // EXISTING METHODS
    public void sendDelivered(int messageId) {
        send("DELIVERED|" + messageId);
    }

    public void markMessagesAsSeen(String sender) {
        send("SEEN|" + encode(sender));
    }

    public void sendProfileUpdate(String username) {
        send("PROFILE_UPDATE|" + encode(username));
    }

    // SEND PROTOCOL MESSAGE
    private void send(String message) {
        if (connected && writer != null) {
            writer.println(message);
        }
    }

    // BASE64 ENCODING
    private String encode(String value) {
        if (value == null) {
            return "";
        }

        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    // CONNECTION STATUS
    public boolean isConnected() {
        return connected;
    }

    // DISCONNECT
    public void disconnect() {
        connected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.out.println(
                    "Disconnect error: " + e.getMessage()
            );
        }
    }
}
