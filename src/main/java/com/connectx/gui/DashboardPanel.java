
package com.connectx.gui;

import com.connectx.client.ChatClient;
import com.connectx.database.MessageDAO;
import com.connectx.database.UserDAO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final JFrame frame;
    private final String username;
    private final Runnable logoutAction;

    private String selectedChatUser = "";

    private JTextField messageField;
    private JTextField receiverField;

    private JPanel messagesContainer;
    private JScrollPane chatScrollPane;

    private JLabel chatTitle;
    private JLabel chatSubtitle;
    private JLabel onlineCountLabel;
    private JLabel chatAvatar;
    private JLabel typingLabel;

    private DefaultListModel<OnlineUser> onlineUsersModel;
    private JList<OnlineUser> onlineUsersList;

    private ChatClient chatClient;
    private boolean connected = false;

    private javax.swing.Timer typingTimer;
    private javax.swing.Timer stopTypingTimer;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Color BG = new Color(9, 11, 24);
    private static final Color SIDEBAR = new Color(15, 19, 39);
    private static final Color PANEL = new Color(24, 29, 54);
    private static final Color CARD = new Color(30, 36, 65);
    private static final Color PURPLE = new Color(139, 92, 246);
    private static final Color PURPLE_DARK = new Color(91, 62, 170);
    private static final Color CYAN = new Color(34, 211, 238);
    private static final Color TEXT = new Color(248, 250, 252);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color GREEN = new Color(52, 211, 153);

    public DashboardPanel(JFrame frame, String username,
                         ChatClient chatClient,
                         Runnable logoutAction) {

        this.frame = frame;
        this.username = username;
        this.chatClient = chatClient;
        this.logoutAction = logoutAction;

        setLayout(new BorderLayout());
        setBackground(BG);

        add(createSidebar(), BorderLayout.WEST);
        add(createContactsPanel(), BorderLayout.EAST);
        add(createChatPanel(), BorderLayout.CENTER);

        loadChatHistory();
        connectToServer();
    }

    // ================= ONLINE USER DATA =================

    private static class OnlineUser {

        String username;
        String displayName;
        String picturePath;
        ImageIcon icon;

        OnlineUser(String username, String displayName,
                   String picturePath) {

            this.username = username;
            this.displayName = displayName;
            this.picturePath = picturePath;
            this.icon = loadProfilePicture(picturePath, displayName);
        }

        private static ImageIcon loadProfilePicture(
                String path, String displayName) {

            if (path != null && !path.isBlank()) {
                File file = new File(path);

                if (file.exists()) {
                    ImageIcon original = new ImageIcon(path);

                    Image image = original.getImage()
                            .getScaledInstance(
                                    44, 44, Image.SCALE_SMOOTH);

                    return new ImageIcon(image);
                }
            }

            BufferedImage image = new BufferedImage(
                    44, 44, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = image.createGraphics();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(PURPLE_DARK);
            g.fillOval(0, 0, 44, 44);

            String initial = displayName == null
                    || displayName.isBlank()
                    ? "?"
                    : displayName.substring(0, 1).toUpperCase();

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));

            FontMetrics fm = g.getFontMetrics();

            int x = (44 - fm.stringWidth(initial)) / 2;
            int y = (44 - fm.getHeight()) / 2 + fm.getAscent();

            g.drawString(initial, x, y);
            g.dispose();

            return new ImageIcon(image);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // ================= INITIAL SCREEN =================

    private void loadChatHistory() {

        clearMessages();

        JLabel welcome = new JLabel("Welcome to ConnectX");
        welcome.setFont(ConnectXTheme.semibold(21));
        welcome.setForeground(TEXT);

        JLabel description = new JLabel(
                "Choose someone from your contacts and start a conversation.");

        description.setFont(ConnectXTheme.regular(12));
        description.setForeground(MUTED);

        JPanel welcomePanel = new JPanel();
        welcomePanel.setOpaque(false);
        welcomePanel.setLayout(
                new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));

        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        welcomePanel.add(Box.createVerticalStrut(150));
        welcomePanel.add(welcome);
        welcomePanel.add(Box.createVerticalStrut(12));
        welcomePanel.add(description);

        messagesContainer.add(welcomePanel);

        refreshMessages();
    }

    // ================= MESSAGE SYSTEM =================

    private void clearMessages() {
        messagesContainer.removeAll();
        messagesContainer.revalidate();
        messagesContainer.repaint();
    }

    private void appendSystemText(String text) {

        JLabel label = new JLabel(text);
        label.setFont(ConnectXTheme.regular(11));
        label.setForeground(MUTED);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        row.add(label);

        messagesContainer.add(row);
        refreshMessages();
    }

    private int historyMessageCount = 0;

    // ================= CHAT BUBBLES =================

    private void appendBubble(String sender, String text, boolean own) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel alignmentPanel = new JPanel(
                new FlowLayout(
                        own ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));

        alignmentPanel.setOpaque(false);

        RoundedBubble bubble = new RoundedBubble(
                own ? PURPLE_DARK : CARD, 20);

        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(11, 15, 11, 15));

        JLabel senderLabel = new JLabel(sender);
        senderLabel.setFont(ConnectXTheme.semibold(10));
        senderLabel.setForeground(own ? CYAN : MUTED);
        senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea messageLabel = new JTextArea(text);
        messageLabel.setEditable(false);
        messageLabel.setFocusable(false);
        messageLabel.setLineWrap(true);
        messageLabel.setWrapStyleWord(true);
        messageLabel.setOpaque(false);
        messageLabel.setForeground(TEXT);
        messageLabel.setFont(ConnectXTheme.regular(13));
        messageLabel.setBorder(null);

        int maxTextWidth = 300;

        int preferredTextWidth = messageLabel
                .getFontMetrics(messageLabel.getFont())
                .stringWidth(text);

        int textWidth = Math.max(
                55, Math.min(maxTextWidth, preferredTextWidth + 5));

        messageLabel.setSize(new Dimension(textWidth, Short.MAX_VALUE));

        Dimension textSize = messageLabel.getPreferredSize();

        messageLabel.setPreferredSize(
                new Dimension(textWidth, textSize.height));

        messageLabel.setMinimumSize(
                new Dimension(textWidth, textSize.height));

        messageLabel.setMaximumSize(
                new Dimension(textWidth, textSize.height));

        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        bubble.add(senderLabel);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(messageLabel);

        int bubbleWidth = textWidth + 30;
        int bubbleHeight = textSize.height + 42;

        bubble.setPreferredSize(
                new Dimension(bubbleWidth, bubbleHeight));

        bubble.setMaximumSize(
                new Dimension(bubbleWidth, bubbleHeight));

        alignmentPanel.add(bubble);
        row.add(alignmentPanel, BorderLayout.CENTER);

        row.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, bubbleHeight + 14));

        messagesContainer.add(row);
        refreshMessages();
    }

    // ================= ATTACHMENT BUBBLE =================

    private void appendAttachmentBubble(
            String sender, String fileName,
            byte[] fileData, boolean own) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel alignment = new JPanel(
                new FlowLayout(
                        own ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));

        alignment.setOpaque(false);

        RoundedBubble bubble = new RoundedBubble(
                own ? PURPLE_DARK : CARD, 18);

        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(13, 15, 13, 15));

        JLabel senderLabel = new JLabel(sender);
        senderLabel.setForeground(own ? CYAN : MUTED);
        senderLabel.setFont(ConnectXTheme.semibold(10));
        senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fileLabel = new JLabel(fileName);
        fileLabel.setForeground(TEXT);
        fileLabel.setFont(ConnectXTheme.semibold(12));
        fileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sizeLabel = new JLabel(formatFileSize(fileData.length));
        sizeLabel.setForeground(MUTED);
        sizeLabel.setFont(ConnectXTheme.regular(10));
        sizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        bubble.add(senderLabel);
        bubble.add(Box.createVerticalStrut(8));

        boolean imageDisplayed = false;

        if (isImageFile(fileName)) {
            try {
                BufferedImage image = ImageIO.read(
                        new ByteArrayInputStream(fileData));

                if (image != null) {
                    int maxWidth = 280;
                    int maxHeight = 200;

                    double scale = Math.min(
                            (double) maxWidth / image.getWidth(),
                            (double) maxHeight / image.getHeight());

                    scale = Math.min(scale, 1.0);

                    int width = Math.max(
                            1, (int) (image.getWidth() * scale));

                    int height = Math.max(
                            1, (int) (image.getHeight() * scale));

                    Image scaledImage = image.getScaledInstance(
                            width, height, Image.SCALE_SMOOTH);

                    JLabel imageLabel = new JLabel(
                            new ImageIcon(scaledImage));

                    imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    imageLabel.setCursor(
                            new Cursor(Cursor.HAND_CURSOR));

                    imageLabel.setToolTipText("Click to view full image");

                    imageLabel.addMouseListener(
                            new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(
                                java.awt.event.MouseEvent e) {
                            previewImage(fileName, fileData);
                        }
                    });

                    bubble.add(imageLabel);
                    bubble.add(Box.createVerticalStrut(8));
                    imageDisplayed = true;
                }
            } catch (Exception e) {
                System.out.println(
                        "Image display error: " + e.getMessage());
            }
        }

        if (imageDisplayed) {
            JLabel imageName = new JLabel(fileName);
            imageName.setForeground(TEXT);
            imageName.setFont(ConnectXTheme.semibold(11));
            imageName.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(imageName);
        } else {
            bubble.add(fileLabel);
        }

        bubble.add(Box.createVerticalStrut(5));
        bubble.add(sizeLabel);
        bubble.add(Box.createVerticalStrut(10));

        if (own) {
            JLabel sentLabel = new JLabel("Attachment sent");
            sentLabel.setForeground(GREEN);
            sentLabel.setFont(ConnectXTheme.regular(10));
            sentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(sentLabel);
        } else {
            JPanel buttons = new JPanel(
                    new FlowLayout(FlowLayout.LEFT, 6, 0));

            buttons.setOpaque(false);
            buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton saveButton =
                    new ConnectXTheme.AuroraButton("Save file");

            saveButton.addActionListener(
                    e -> saveAttachment(fileName, fileData));

            buttons.add(saveButton);
            bubble.add(buttons);
        }

        alignment.add(bubble);
        row.add(alignment, BorderLayout.CENTER);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 285));

        messagesContainer.add(row);
        refreshMessages();
    }

    private String formatFileSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " bytes";
        }

        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }

        return String.format("%.2f MB",
                bytes / (1024.0 * 1024.0));
    }

    private boolean isImageFile(String fileName) {

        String name = fileName.toLowerCase();

        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".gif")
                || name.endsWith(".webp");
    }

    private void saveAttachment(String fileName, byte[] data) {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Attachment");
        chooser.setSelectedFile(new File(fileName));

        int result = chooser.showSaveDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File destination = chooser.getSelectedFile();

            if (destination.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                        frame,
                        "This file already exists. Replace it?",
                        "Confirm Save",
                        JOptionPane.YES_NO_OPTION);

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                Files.write(destination.toPath(), data);

                showMessage(
                        "File saved successfully:\n"
                                + destination.getAbsolutePath());

            } catch (Exception e) {
                showMessage("Could not save file: " + e.getMessage());
            }
        }
    }

    private void previewImage(String fileName, byte[] data) {

        try {
            BufferedImage image = ImageIO.read(
                    new ByteArrayInputStream(data));

            if (image == null) {
                showMessage("This image format cannot be previewed.");
                return;
            }

            int maxWidth = 700;
            int maxHeight = 550;

            double scale = Math.min(
                    (double) maxWidth / image.getWidth(),
                    (double) maxHeight / image.getHeight());

            int width = Math.max(1, (int) (image.getWidth() * scale));
            int height = Math.max(1, (int) (image.getHeight() * scale));

            Image scaled = image.getScaledInstance(
                    width, height, Image.SCALE_SMOOTH);

            JLabel preview = new JLabel(new ImageIcon(scaled));

            JOptionPane.showMessageDialog(
                    frame,
                    new JScrollPane(preview),
                    fileName,
                    JOptionPane.PLAIN_MESSAGE);

        } catch (Exception e) {
            showMessage("Could not preview this image.");
        }
    }

    // ================= ROUNDED BUBBLE =================

    private static class RoundedBubble extends JPanel {

        private final Color bubbleColor;
        private final int radius;

        RoundedBubble(Color color, int radius) {
            this.bubbleColor = color;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(bubbleColor);

            g2.fillRoundRect(
                    0, 0, getWidth(), getHeight(),
                    radius, radius);

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // ================= REFRESH MESSAGES =================

    private void refreshMessages() {

        messagesContainer.revalidate();
        messagesContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            if (chatScrollPane != null) {
                JScrollBar bar =
                        chatScrollPane.getVerticalScrollBar();

                bar.setValue(bar.getMaximum());
            }
        });
    }

    private void appendHistoryMessage(String historyLine) {

        int separator = historyLine.indexOf(':');

        if (separator <= 0) {
            appendSystemText(historyLine);
            return;
        }

        String sender = historyLine
                .substring(0, separator).trim();

        String text = historyLine
                .substring(separator + 1).trim();

        appendBubble(
                sender, text,
                sender.equalsIgnoreCase(username));
    }

    // ================= OPEN PRIVATE CHAT =================

    private void openChat() {

        String receiver = receiverField.getText().trim();

        if (receiver.isEmpty()) {
            showMessage("Please select or enter a username.");
            return;
        }

        if (receiver.equalsIgnoreCase(username)) {
            showMessage("You cannot chat with yourself.");
            return;
        }

        if (!UserDAO.userExists(receiver)) {
            showMessage(
                    "This username is not registered in ConnectX.");
            return;
        }

        stopTyping();

        selectedChatUser = receiver;

        UserDAO.UserProfile profile =
                UserDAO.getUserProfile(receiver);

        String displayName = receiver;

        if (profile != null
                && profile.getDisplayName() != null
                && !profile.getDisplayName().isBlank()) {
            displayName = profile.getDisplayName();
        }

        chatTitle.setText(displayName);
        chatSubtitle.setText("Private conversation");
        chatSubtitle.setForeground(GREEN);

        chatAvatar.setText(
                displayName.substring(0, 1).toUpperCase());

        typingLabel.setText(" ");

        clearMessages();

        historyMessageCount = 0;

        if (chatClient != null && chatClient.isConnected()) {
            chatClient.requestHistory(receiver);
        } else {
            appendSystemText("Not connected to the chat server.");
        }

        refreshMessages();
    }

    private void selectOnlineUser() {

        OnlineUser selectedUser =
                onlineUsersList.getSelectedValue();

        if (selectedUser == null) {
            return;
        }

        receiverField.setText(selectedUser.username);
        openChat();
    }

    // ================= BASE64 DECODING =================

    private String decode(String value) {

        try {
            return new String(
                    Base64.getUrlDecoder().decode(value),
                    StandardCharsets.UTF_8);

        } catch (Exception e) {
            try {
                return new String(
                        Base64.getDecoder().decode(value),
                        StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    // ================= ONLINE USERS =================

    private void updateOnlineUsers(String message) {

        String usersText = message.substring(9);

        onlineUsersModel.clear();

        if (!usersText.isEmpty()) {

            String[] entries = usersText.split(",");

            List<OnlineUser> users = new ArrayList<>();

            for (String entry : entries) {

                String[] parts = entry.split(":", 3);

                if (parts.length != 3) {
                    continue;
                }

                String user = decode(parts[0]);
                String displayName = decode(parts[1]);
                String picturePath = decode(parts[2]);

                if (user.isBlank()
                        || user.equalsIgnoreCase(username)) {
                    continue;
                }

                if (displayName.isBlank()) {
                    displayName = user;
                }

                users.add(new OnlineUser(
                        user, displayName, picturePath));
            }

            users.sort(Comparator.comparing(
                    user -> user.displayName,
                    String.CASE_INSENSITIVE_ORDER));

            for (OnlineUser user : users) {
                onlineUsersModel.addElement(user);
            }
        }

        onlineCountLabel.setText(
                onlineUsersModel.size() + " ONLINE");
    }

    // ================= TYPING SYSTEM =================

    private void startTypingDetection() {

        typingTimer = new javax.swing.Timer(500, e -> {
            if (!selectedChatUser.isEmpty() && connected) {
                chatClient.sendTyping(username, selectedChatUser);
            }
        });

        typingTimer.setRepeats(false);

        stopTypingTimer = new javax.swing.Timer(1500, e -> {
            if (!selectedChatUser.isEmpty() && connected) {
                chatClient.stopTyping(username, selectedChatUser);
            }
        });

        stopTypingTimer.setRepeats(false);

        messageField.getDocument().addDocumentListener(
                new DocumentListener() {

            private void changed() {
                if (!selectedChatUser.isEmpty() && connected) {
                    typingTimer.restart();
                    stopTypingTimer.restart();
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
    }

    private void stopTyping() {

        if (typingTimer != null) {
            typingTimer.stop();
        }

        if (stopTypingTimer != null) {
            stopTypingTimer.stop();
        }

        if (connected && chatClient != null
                && !selectedChatUser.isEmpty()) {
            chatClient.stopTyping(username, selectedChatUser);
        }
    }

    // ================= MESSAGE SEARCH =================

    private void searchMessages() {

        if (selectedChatUser.isEmpty()) {
            showMessage("Open a conversation before searching.");
            return;
        }

        String query = JOptionPane.showInputDialog(
                frame,
                "Enter a word or phrase to search:",
                "Search Messages",
                JOptionPane.QUESTION_MESSAGE);

        if (query == null || query.trim().isEmpty()) {
            return;
        }

        if (!connected) {
            showMessage("Not connected to the server.");
            return;
        }

        chatClient.searchMessages(
                username, selectedChatUser, query.trim());

        appendSystemText(
                "Searching messages for: " + query.trim());
    }

    private void displaySearchResult(String message) {

        String[] parts = message.split("\\|", 6);

        if (parts.length < 6) {
            return;
        }

        String sender = decode(parts[2]);
        String text = decode(parts[4]);
        String sentAt = decode(parts[5]);

        appendBubble(
                sender + "  •  " + sentAt,
                text,
                sender.equalsIgnoreCase(username));
    }

    // ================= FILE SHARING =================

    private void chooseAndSendFile() {

        if (selectedChatUser.isEmpty()) {
            showMessage("Please open a conversation first.");
            return;
        }

        if (!connected) {
            showMessage("Not connected to the server.");
            return;
        }

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Choose a file to share");

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Supported files (Images, PDF, Documents)",
                        "png", "jpg", "jpeg", "gif", "webp",
                        "pdf", "txt", "doc", "docx",
                        "xls", "xlsx", "ppt", "pptx"));

        int result = chooser.showOpenDialog(frame);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        if (!file.isFile()) {
            showMessage("Please select a valid file.");
            return;
        }

        if (file.length() == 0) {
            showMessage("This file is empty.");
            return;
        }

        if (file.length() > MAX_FILE_SIZE) {
            showMessage("File size must be 5 MB or less.");
            return;
        }

        String fileName = file.getName();

        if (!isAllowedFile(fileName)) {
            showMessage("This file type is not supported.");
            return;
        }

        try {

            byte[] fileBytes =
                    Files.readAllBytes(file.toPath());

            String mimeType =
                    Files.probeContentType(file.toPath());

            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            String fileData =
                    Base64.getEncoder().encodeToString(fileBytes);

            chatClient.sendFile(
                    username,
                    selectedChatUser,
                    fileName,
                    mimeType,
                    fileData);

            appendAttachmentBubble(
                    "You", fileName, fileBytes, true);

            appendSystemText(
                    "Sending attachment to " + selectedChatUser);

        } catch (Exception e) {
            showMessage("Could not send file: " + e.getMessage());
        }
    }

    private boolean isAllowedFile(String fileName) {

        String lower = fileName.toLowerCase();

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

    private void handleReceivedFile(String message) {

        String[] parts = message.split("\\|", 7);

        if (parts.length != 7) {
            showMessage("Received an invalid attachment.");
            return;
        }

        String sender = parts[2];
        String receiver = parts[3];
        String fileName = decode(parts[4]);

        if (!receiver.equalsIgnoreCase(username)) {
            return;
        }

        if (!sender.equalsIgnoreCase(selectedChatUser)) {
            showMessage(
                    sender + " sent you a file. Open their chat to receive it.");
            return;
        }

        try {
            byte[] fileBytes =
                    Base64.getDecoder().decode(parts[6]);

            appendAttachmentBubble(
                    sender, fileName, fileBytes, false);

        } catch (Exception e) {
            showMessage("Could not read the received attachment.");
        }
    }

    // ================= SERVER CONNECTION =================

    private void connectToServer() {



        chatClient.setMessageListener(message -> {

            SwingUtilities.invokeLater(() -> {

                if (message.startsWith("HISTORY_ITEM|")) {

                    try {
                        String[] parts = message.split("\\|", -1);

                        if (parts.length >= 8) {
                            String sender = decode(parts[2]);
                            String text = decode(parts[3]);
                            String fileName = decode(parts[5]);

                            boolean own = sender.equalsIgnoreCase(username);

                            if (!fileName.isBlank()) {
                                byte[] fileData;

                                try {
                                    fileData = Base64.getDecoder().decode(parts[7]);
                                } catch (Exception ex) {
                                    fileData = Base64.getUrlDecoder().decode(parts[7]);
                                }

                                appendAttachmentBubble(
                                        sender, fileName, fileData, own);
                            }

                            if (!text.isBlank()) {
                                appendBubble(sender, text, own);
                            }

                            historyMessageCount++;
                        }
                    } catch (Exception ex) {
                        System.err.println(
                                "Could not display history item: " + ex.getMessage());
                    }

                } else if (message.equals("HISTORY_DONE")) {

                    if (historyMessageCount == 0) {
                        appendSystemText(
                                "No previous messages. Say hello and start chatting!");
                    }

                    refreshMessages();

                } else if (message.startsWith("USERLIST|")) {

                    updateOnlineUsers(message);

                } else if (message.startsWith("PRIVATE|")) {

                    String[] parts = message.split("\\|", 5);

                    if (parts.length == 5) {

                        String sender = parts[2];
                        String receiver = parts[3];
                        String text = parts[4];

                        if (receiver.equalsIgnoreCase(username)
                                && sender.equalsIgnoreCase(selectedChatUser)) {
                            appendBubble(sender, text, false);
                        }
                    }

                } else if (message.startsWith("FILE_RECEIVED|")) {

                    handleReceivedFile(message);

                } else if (message.startsWith("FILE_SENT|")) {

                    appendSystemText("Attachment sent successfully.");

                } else if (message.startsWith("TYPING|")) {

                    String sender = message.substring(7).trim();

                    if (sender.equalsIgnoreCase(selectedChatUser)) {
                        typingLabel.setText(sender + " is typing...");
                    }

                } else if (message.startsWith("STOP_TYPING|")) {

                    String sender = message.substring(12).trim();

                    if (sender.equalsIgnoreCase(selectedChatUser)) {
                        typingLabel.setText(" ");
                    }

                } else if (message.startsWith("SEARCH_RESULT|")) {

                    displaySearchResult(message);

                } else if (message.equals("SEARCH_DONE")) {

                    appendSystemText("Search completed.");

                } else if (message.startsWith("OFFLINE|")) {

                    String receiver = message.substring(8);

                    showMessage(
                            receiver + " is currently offline.\n"
                                    + "Your message has been saved.");

                } else if (message.startsWith("SYSTEM|")) {

                    System.out.println(message.substring(7));

                } else if (message.startsWith("ERROR|")) {

                    showMessage(message.substring(6));
                }

                // SENT and STATUS messages intentionally ignored.
            });
        });

    }

    public void markConnected() {
        connected = chatClient != null && chatClient.isConnected();
    }

    // ================= CUSTOM SIDEBAR ICON =================

    private static class SidebarIcon implements Icon {

        private final String type;
        private final Color color;

        SidebarIcon(String type, Color color) {
            this.type = type;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return 23;
        }

        @Override
        public int getIconHeight() {
            return 23;
        }

        @Override
        public void paintIcon(
                Component component, Graphics graphics,
                int x, int y) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.translate(x, y);

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(color);

            g.setStroke(new BasicStroke(
                    2.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            switch (type) {

                case "brand" -> {
                    g.drawLine(3, 3, 20, 20);
                    g.drawLine(20, 3, 3, 20);
                }

                case "home" -> {
                    Polygon roof = new Polygon(
                            new int[]{2, 11, 20},
                            new int[]{11, 3, 11},
                            3);

                    g.drawPolygon(roof);
                    g.drawRoundRect(5, 10, 12, 11, 2, 2);
                }

                case "chat" -> {
                    g.drawRoundRect(2, 3, 19, 14, 5, 5);
                    g.drawLine(7, 17, 5, 21);
                    g.drawLine(5, 21, 11, 17);

                    g.fillOval(6, 9, 2, 2);
                    g.fillOval(10, 9, 2, 2);
                    g.fillOval(14, 9, 2, 2);
                }

                case "user" -> {
                    g.drawOval(8, 2, 8, 8);
                    g.drawArc(3, 11, 18, 15, 0, 180);
                }

                case "logout" -> {
                    g.drawRoundRect(2, 3, 12, 17, 3, 3);
                    g.drawLine(10, 11, 21, 11);
                    g.drawLine(17, 7, 21, 11);
                    g.drawLine(17, 15, 21, 11);
                }
            }

            g.dispose();
        }
    }

    // ================= SIDEBAR =================

    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();

        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBackground(SIDEBAR);

        sidebar.setLayout(
                new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        sidebar.setBorder(
                new EmptyBorder(30, 18, 25, 18));

        JPanel logoPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0));

        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoIcon = new JLabel(
                new SidebarIcon("brand", CYAN));

        JLabel logo = ConnectXTheme.createHeading(
                "ConnectX", 27);

        logo.setForeground(CYAN);

        logoPanel.add(logoIcon);
        logoPanel.add(logo);

        JLabel tagline = new JLabel("YOUR SPACE TO CONNECT");
        tagline.setFont(ConnectXTheme.semibold(9));
        tagline.setForeground(MUTED);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel accountHeading = smallHeading("YOUR ACCOUNT");

        JLabel usernameLabel =
                ConnectXTheme.createHeading(username, 14);

        usernameLabel.setForeground(TEXT);

        JPanel accountCard = new JPanel();

        accountCard.setLayout(
                new BoxLayout(accountCard, BoxLayout.Y_AXIS));

        accountCard.setBackground(PANEL);
        accountCard.setBorder(
                new EmptyBorder(15, 13, 15, 13));

        accountCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        accountCard.add(accountHeading);
        accountCard.add(Box.createVerticalStrut(10));
        accountCard.add(usernameLabel);

        JLabel menuHeading = smallHeading("WORKSPACE");

        JButton dashboardButton =
                navButton("home", "Overview", true);

        JButton messagesButton =
                navButton("chat", "Messages", false);

        JButton profileButton =
                navButton("user", "My Profile", false);

        profileButton.addActionListener(e -> {

            ProfileDialog dialog =
                    new ProfileDialog(frame, username, chatClient);

            dialog.setVisible(true);
        });

        JButton logoutButton =
                navButton("logout", "Sign out", false);

        logoutButton.setForeground(
                new Color(255, 125, 145));

        logoutButton.setIcon(
                new SidebarIcon(
                        "logout", new Color(255, 125, 145)));

        logoutButton.addActionListener(e -> {

            stopTyping();

            if (chatClient != null) {
                chatClient.disconnect();
            }

            connected = false;
            logoutAction.run();
        });

        sidebar.add(logoPanel);
        sidebar.add(Box.createVerticalStrut(7));
        sidebar.add(tagline);

        sidebar.add(Box.createVerticalStrut(42));
        sidebar.add(accountCard);

        sidebar.add(Box.createVerticalStrut(38));
        sidebar.add(menuHeading);
        sidebar.add(Box.createVerticalStrut(18));

        sidebar.add(dashboardButton);
        sidebar.add(Box.createVerticalStrut(9));

        sidebar.add(messagesButton);
        sidebar.add(Box.createVerticalStrut(9));

        sidebar.add(profileButton);

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutButton);

        return sidebar;
    }

    private JLabel smallHeading(String text) {

        JLabel label = new JLabel(text);

        label.setFont(ConnectXTheme.semibold(9));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    // ================= NAVIGATION BUTTON =================

    private JButton navButton(
            String iconType, String text, boolean selected) {

        JButton button = new JButton(text);

        button.setFont(ConnectXTheme.semibold(13));
        button.setHorizontalAlignment(SwingConstants.LEFT);

        button.setIcon(new SidebarIcon(
                iconType, selected ? CYAN : MUTED));

        button.setIconTextGap(17);
        button.setForeground(selected ? CYAN : MUTED);

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);

        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 48));

        button.setPreferredSize(
                new Dimension(175, 48));

        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.setBorder(
                new EmptyBorder(10, 13, 10, 10));

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(
                    java.awt.event.MouseEvent e) {

                if (!selected) {
                    button.setForeground(CYAN);
                    button.setIcon(
                            new SidebarIcon(iconType, CYAN));
                }
            }

            @Override
            public void mouseExited(
                    java.awt.event.MouseEvent e) {

                if (!selected) {
                    button.setForeground(MUTED);
                    button.setIcon(
                            new SidebarIcon(iconType, MUTED));
                }
            }
        });

        return button;
    }

    // ================= CONTACTS PANEL =================

    private JPanel createContactsPanel() {

        JPanel panel = new JPanel(
                new BorderLayout(0, 18));

        panel.setPreferredSize(new Dimension(235, 0));
        panel.setBackground(BG);

        panel.setBorder(
                new EmptyBorder(30, 17, 25, 17));

        JLabel heading =
                ConnectXTheme.createHeading("Messages", 22);

        JLabel description =
                ConnectXTheme.createLabel(
                        "Your people, your conversations", 10);

        description.setForeground(MUTED);

        onlineCountLabel = new JLabel("0 ONLINE");

        onlineCountLabel.setFont(
                ConnectXTheme.semibold(10));

        onlineCountLabel.setForeground(GREEN);

        JPanel top = new JPanel();

        top.setOpaque(false);

        top.setLayout(
                new BoxLayout(top, BoxLayout.Y_AXIS));

        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        onlineCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(heading);
        top.add(Box.createVerticalStrut(6));
        top.add(description);
        top.add(Box.createVerticalStrut(25));
        top.add(onlineCountLabel);

        onlineUsersModel = new DefaultListModel<>();
        onlineUsersList = new JList<>(onlineUsersModel);

        onlineUsersList.setBackground(BG);
        onlineUsersList.setForeground(TEXT);

        onlineUsersList.setSelectionBackground(
                new Color(45, 39, 78));

        onlineUsersList.setSelectionForeground(TEXT);
        onlineUsersList.setFixedCellHeight(76);

        onlineUsersList.setFont(
                ConnectXTheme.regular(12));

        onlineUsersList.setCellRenderer(
                new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value,
                    int index, boolean isSelected,
                    boolean cellHasFocus) {

                JLabel label = (JLabel)
                        super.getListCellRendererComponent(
                                list, value, index,
                                isSelected, cellHasFocus);

                if (value instanceof OnlineUser user) {

                    label.setText(
                            "<html><b>"
                                    + escapeHtml(user.displayName)
                                    + "</b><br><font color='#94A3B8' size='2'>"
                                    + escapeHtml(user.username)
                                    + "</font><br><font color='#34D399' size='2'>"
                                    + "Online</font></html>");

                    label.setIcon(user.icon);
                    label.setIconTextGap(12);
                }

                label.setForeground(TEXT);

                label.setBackground(
                        isSelected
                                ? new Color(45, 39, 78)
                                : BG);

                label.setBorder(
                        new EmptyBorder(11, 9, 11, 5));

                return label;
            }
        });

        onlineUsersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectOnlineUser();
            }
        });

        JScrollPane scrollPane =
                new JScrollPane(onlineUsersList);

        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        JLabel bottomText =
                ConnectXTheme.createLabel(
                        "Select a contact to start chatting.", 10);

        bottomText.setForeground(MUTED);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomText, BorderLayout.SOUTH);

        return panel;
    }

    private String escapeHtml(String text) {

        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ================= MAIN CHAT PANEL =================

    private JPanel createChatPanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(22, 10, 22, 10));

        // HEADER
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(PANEL);
        header.setBorder(new EmptyBorder(14, 16, 14, 16));

        // CHAT IDENTITY
        JPanel identity = new JPanel(new BorderLayout(12, 0));
        identity.setOpaque(false);

        chatAvatar = new JLabel("CX", SwingConstants.CENTER);
        chatAvatar.setOpaque(true);
        chatAvatar.setBackground(new Color(53, 43, 94));
        chatAvatar.setForeground(CYAN);
        chatAvatar.setFont(ConnectXTheme.semibold(16));
        chatAvatar.setPreferredSize(new Dimension(46, 46));
        chatAvatar.setMinimumSize(new Dimension(46, 46));
        chatAvatar.setBorder(BorderFactory.createLineBorder(
                new Color(139, 92, 246, 100), 1));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(
                new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        chatTitle = ConnectXTheme.createHeading(
                "Welcome to ConnectX", 15);
        chatTitle.setForeground(TEXT);

        chatSubtitle = ConnectXTheme.createLabel(
                "Select a contact to begin", 10);
        chatSubtitle.setForeground(MUTED);

        chatTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        chatSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titlePanel.add(chatTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(chatSubtitle);

        identity.add(chatAvatar, BorderLayout.WEST);
        identity.add(titlePanel, BorderLayout.CENTER);

        // RECIPIENT CONTROLS
        JPanel recipientPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 7, 2));
        recipientPanel.setOpaque(false);

        receiverField =
                ConnectXTheme.createTextField("Username");

        receiverField.setPreferredSize(new Dimension(145, 40));
        receiverField.setMinimumSize(new Dimension(120, 40));
        receiverField.setFont(ConnectXTheme.regular(12));

        JButton openButton =
                new ConnectXTheme.AuroraButton("Open");

        openButton.setPreferredSize(new Dimension(82, 40));
        openButton.setMinimumSize(new Dimension(82, 40));
        openButton.setMargin(new Insets(5, 10, 5, 10));
        openButton.setFont(ConnectXTheme.semibold(12));
        openButton.addActionListener(e -> openChat());

        receiverField.addActionListener(e -> openChat());

        JButton searchButton =
                new ConnectXTheme.AuroraButton("Find");

        searchButton.setPreferredSize(new Dimension(75, 40));
        searchButton.setMinimumSize(new Dimension(75, 40));
        searchButton.setMargin(new Insets(5, 10, 5, 10));
        searchButton.setFont(ConnectXTheme.semibold(12));
        searchButton.setToolTipText("Search messages");
        searchButton.addActionListener(e -> searchMessages());

        recipientPanel.add(receiverField);
        recipientPanel.add(openButton);
        recipientPanel.add(searchButton);

        header.add(identity, BorderLayout.CENTER);
        header.add(recipientPanel, BorderLayout.EAST);

        // TYPING INDICATOR
        typingLabel = new JLabel(" ");
        typingLabel.setFont(ConnectXTheme.regular(11));
        typingLabel.setForeground(CYAN);
        typingLabel.setBorder(new EmptyBorder(2, 8, 2, 8));

        JPanel headerContainer = new JPanel(
                new BorderLayout(0, 5));
        headerContainer.setOpaque(false);

        headerContainer.add(header, BorderLayout.CENTER);
        headerContainer.add(typingLabel, BorderLayout.SOUTH);

        // MESSAGE AREA
        messagesContainer = new JPanel();
        messagesContainer.setLayout(
                new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(BG);
        messagesContainer.setBorder(
                new EmptyBorder(15, 12, 15, 12));

        chatScrollPane = new JScrollPane(messagesContainer);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(
                new Color(38, 45, 75), 1));
        chatScrollPane.getViewport().setBackground(BG);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(18);

        // MESSAGE COMPOSER
        JPanel composer = new JPanel(
                new BorderLayout(12, 0));
        composer.setBackground(PANEL);
        composer.setBorder(
                new EmptyBorder(13, 15, 13, 15));

        messageField =
                ConnectXTheme.createTextField("Write a message...");

        messageField.setFont(ConnectXTheme.regular(13));

        JButton attachButton =
                new ConnectXTheme.AuroraButton("File");

        attachButton.setPreferredSize(new Dimension(78, 44));
        attachButton.setMinimumSize(new Dimension(78, 44));
        attachButton.setMargin(new Insets(5, 10, 5, 10));
        attachButton.setFont(ConnectXTheme.semibold(12));
        attachButton.setToolTipText(
                "Attach image or document");
        attachButton.addActionListener(
                e -> chooseAndSendFile());

        JButton sendButton =
                new ConnectXTheme.AuroraButton("Send");

        sendButton.setPreferredSize(new Dimension(88, 44));
        sendButton.setMinimumSize(new Dimension(88, 44));
        sendButton.setMargin(new Insets(5, 10, 5, 10));
        sendButton.setFont(ConnectXTheme.semibold(12));
        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        composer.add(attachButton, BorderLayout.WEST);
        composer.add(messageField, BorderLayout.CENTER);
        composer.add(sendButton, BorderLayout.EAST);

        panel.add(headerContainer, BorderLayout.NORTH);
        panel.add(chatScrollPane, BorderLayout.CENTER);
        panel.add(composer, BorderLayout.SOUTH);

        startTypingDetection();

        return panel;
    }

    // ================= SEND MESSAGE =================

    private void sendMessage() {

        String receiver = receiverField.getText().trim();
        String message = messageField.getText().trim();

        if (selectedChatUser.isEmpty()
                || !receiver.equalsIgnoreCase(selectedChatUser)) {

            showMessage(
                    "Please open a conversation before sending a message.");

            return;
        }

        if (message.isEmpty()) {
            return;
        }

        if (!connected) {
            showMessage("Not connected to the server!");
            return;
        }

        stopTyping();

        appendBubble("You", message, true);

        chatClient.sendPrivateMessage(
                username, selectedChatUser, message);

        messageField.setText("");
        messageField.requestFocusInWindow();
    }

    // ================= COMMON MESSAGE =================

    private void showMessage(String message) {

        JOptionPane.showMessageDialog(
                frame,
                message,
                "ConnectX",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= OPEN DASHBOARD =================

    public static void showDashboard(
            JFrame frame, String username,
            ChatClient chatClient,
            Runnable logoutAction) {

        DashboardPanel dashboard = new DashboardPanel(
                frame, username, chatClient, logoutAction);

        frame.setContentPane(dashboard);
        frame.revalidate();
        frame.repaint();
    }
}
