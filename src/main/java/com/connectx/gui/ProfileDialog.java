
package com.connectx.gui;

import com.connectx.client.ChatClient;
import com.connectx.database.UserDAO;
import com.connectx.database.UserDAO.UserProfile;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class ProfileDialog extends JDialog {

    private final String username;
    private final ChatClient chatClient;

    private JTextField displayNameField;
    private JTextArea bioArea;

    private JLabel usernameLabel;
    private JLabel emailLabel;
    private JLabel pictureLabel;
    private JLabel statusLabel;

    private String selectedPicturePath = null;

    // Midnight Aurora colors
    private final Color BG = new Color(12, 15, 30);
    private final Color PANEL = new Color(24, 30, 52);
    private final Color FIELD = new Color(31, 39, 65);

    private final Color PURPLE = new Color(139, 92, 246);
    private final Color CYAN = new Color(34, 211, 238);

    private final Color WHITE = new Color(245, 247, 255);
    private final Color MUTED = new Color(155, 166, 190);

    public ProfileDialog(
            JFrame parent,
            String username,
            ChatClient chatClient) {

        super(parent, "ConnectX - My Profile", true);

        this.username = username;
        this.chatClient = chatClient;

        setSize(560, 760);
        setMinimumSize(new Dimension(500, 650));
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
        loadProfile();
    }

    // MAIN UI

    private void buildUI() {

        JPanel root = new GradientPanel(
            new Color(20, 19, 48),
            BG
        );

        root.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(
            new BoxLayout(content, BoxLayout.Y_AXIS)
        );

        content.setBorder(
            new EmptyBorder(28, 35, 28, 35)
        );

        // Heading

        JLabel heading = new JLabel("My Profile");
        heading.setFont(
            new Font("Segoe UI", Font.BOLD, 30)
        );
        heading.setForeground(WHITE);

        JLabel subtitle = new JLabel(
            "Manage your ConnectX account"
        );
        subtitle.setFont(
            new Font("Segoe UI", Font.PLAIN, 14)
        );
        subtitle.setForeground(MUTED);

        addLeft(content, heading);
        addGap(content, 5);
        addLeft(content, subtitle);

        addGap(content, 24);

        // Profile picture section

        JPanel pictureCard = new JPanel();
        pictureCard.setOpaque(false);
        pictureCard.setLayout(
            new BoxLayout(pictureCard, BoxLayout.Y_AXIS)
        );

        pictureCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        pictureLabel = new JLabel("CX");
        pictureLabel.setFont(
            new Font("Segoe UI", Font.BOLD, 32)
        );
        pictureLabel.setForeground(WHITE);
        pictureLabel.setHorizontalAlignment(
            SwingConstants.CENTER
        );
        pictureLabel.setVerticalAlignment(
            SwingConstants.CENTER
        );
        pictureLabel.setOpaque(true);
        pictureLabel.setBackground(
            new Color(83, 61, 145)
        );
        pictureLabel.setPreferredSize(
            new Dimension(115, 115)
        );
        pictureLabel.setMinimumSize(
            new Dimension(115, 115)
        );
        pictureLabel.setMaximumSize(
            new Dimension(115, 115)
        );
        pictureLabel.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );

        pictureLabel.setBorder(
            BorderFactory.createLineBorder(CYAN, 2)
        );

        JButton chooseButton =
            createSecondaryButton("CHANGE PROFILE PICTURE");

        chooseButton.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );

        chooseButton.addActionListener(
            e -> choosePicture()
        );

        pictureCard.add(pictureLabel);
        pictureCard.add(Box.createVerticalStrut(13));
        pictureCard.add(chooseButton);

        JPanel pictureWrapper = new JPanel(
            new FlowLayout(FlowLayout.CENTER)
        );
        pictureWrapper.setOpaque(false);
        pictureWrapper.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        pictureWrapper.add(pictureCard);

        content.add(pictureWrapper);

        addGap(content, 25);

        // Account information

        addSection(content, "ACCOUNT INFORMATION");

        usernameLabel = createValueLabel();
        emailLabel = createValueLabel();

        content.add(
            createInfoCard("Username", usernameLabel)
        );

        addGap(content, 10);

        content.add(
            createInfoCard("Email Address", emailLabel)
        );

        addGap(content, 25);

        // Display name

        addSection(content, "DISPLAY NAME");

        displayNameField = new JTextField();
        styleTextField(displayNameField);

        addGap(content, 9);
        content.add(displayNameField);

        addGap(content, 22);

        // Bio

        addSection(content, "ABOUT YOU");

        bioArea = new JTextArea(4, 20);
        bioArea.setFont(
            new Font("Segoe UI", Font.PLAIN, 14)
        );
        bioArea.setForeground(WHITE);
        bioArea.setBackground(FIELD);
        bioArea.setCaretColor(CYAN);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        bioArea.setBorder(
            new EmptyBorder(12, 14, 12, 14)
        );

        JScrollPane bioScroll = new JScrollPane(bioArea);
        bioScroll.setBorder(
            new RoundedBorder(new Color(65, 77, 110), 12)
        );
        bioScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        bioScroll.setPreferredSize(
            new Dimension(400, 100)
        );
        bioScroll.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 100)
        );

        addGap(content, 9);
        content.add(bioScroll);

        addGap(content, 25);

        // Save button

        JButton saveButton =
            createPrimaryButton("SAVE CHANGES");

        saveButton.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        saveButton.addActionListener(
            e -> saveProfile()
        );

        content.add(saveButton);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(
            new Font("Segoe UI", Font.PLAIN, 13)
        );
        statusLabel.setForeground(CYAN);
        statusLabel.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        addGap(content, 12);
        content.add(statusLabel);

        // Footer

        addGap(content, 20);

        JLabel footer = new JLabel(
            "ConnectX  •  Connect. Chat. Share."
        );
        footer.setFont(
            new Font("Segoe UI", Font.ITALIC, 12)
        );
        footer.setForeground(MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(footer);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        root.add(scrollPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    // SECTION LABEL

    private void addSection(JPanel panel, String text) {

        JLabel label = new JLabel(text);

        label.setFont(
            new Font("Segoe UI", Font.BOLD, 12)
        );

        label.setForeground(CYAN);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
    }

    private void addLeft(JPanel panel, JComponent component) {

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(component);
    }

    private void addGap(JPanel panel, int height) {

        panel.add(Box.createVerticalStrut(height));
    }

    // ACCOUNT INFORMATION CARD

    private JLabel createValueLabel() {

        JLabel label = new JLabel();

        label.setFont(
            new Font("Segoe UI", Font.BOLD, 14)
        );

        label.setForeground(WHITE);

        return label;
    }

    private JPanel createInfoCard(
            String title,
            JLabel valueLabel) {

        JPanel card = new JPanel(
            new BorderLayout(12, 0)
        );

        card.setBackground(PANEL);
        card.setBorder(
            new CompoundBorder(
                new RoundedBorder(
                    new Color(55, 67, 103), 12
                ),
                new EmptyBorder(13, 15, 13, 15)
            )
        );

        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 55)
        );

        JLabel titleLabel = new JLabel(title);

        titleLabel.setFont(
            new Font("Segoe UI", Font.PLAIN, 13)
        );

        titleLabel.setForeground(MUTED);

        card.add(titleLabel, BorderLayout.WEST);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // TEXT FIELD

    private void styleTextField(JTextField field) {

        field.setFont(
            new Font("Segoe UI", Font.PLAIN, 14)
        );

        field.setForeground(WHITE);
        field.setBackground(FIELD);
        field.setCaretColor(CYAN);

        field.setBorder(
            new CompoundBorder(
                new RoundedBorder(
                    new Color(65, 77, 110), 12
                ),
                new EmptyBorder(11, 14, 11, 14)
            )
        );

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 45)
        );
    }

    // BUTTONS

    private JButton createPrimaryButton(String text) {

        JButton button = new JButton(text) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                Color first = PURPLE;
                Color second = CYAN;

                if (getModel().isPressed()) {
                    first = first.darker();
                    second = second.darker();
                }

                g2.setPaint(
                    new GradientPaint(
                        0, 0, first,
                        getWidth(), getHeight(), second
                    )
                );

                g2.fillRoundRect(
                    0, 0, getWidth(), getHeight(), 14, 14
                );

                g2.dispose();

                super.paintComponent(g);
            }
        };

        button.setFont(
            new Font("Segoe UI", Font.BOLD, 13)
        );

        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorderPainted(false);

        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        button.setPreferredSize(
            new Dimension(200, 46)
        );

        button.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 46)
        );

        return button;
    }

    private JButton createSecondaryButton(String text) {

        JButton button = new JButton(text);

        button.setFont(
            new Font("Segoe UI", Font.BOLD, 11)
        );

        button.setForeground(CYAN);
        button.setBackground(PANEL);
        button.setFocusPainted(false);
        button.setOpaque(true);

        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        button.setBorder(
            new CompoundBorder(
                new RoundedBorder(
                    new Color(75, 105, 150), 12
                ),
                new EmptyBorder(10, 17, 10, 17)
            )
        );

        return button;
    }

    // LOAD PROFILE

    private void loadProfile() {

        UserProfile profile =
            UserDAO.getUserProfile(username);

        if (profile == null) {

            JOptionPane.showMessageDialog(
                this,
                "Could not load profile information.",
                "ConnectX",
                JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        usernameLabel.setText(profile.getUsername());
        emailLabel.setText(profile.getEmail());

        String displayName = profile.getDisplayName();

        if (displayName == null || displayName.isBlank()) {
            displayName = profile.getUsername();
        }

        displayNameField.setText(displayName);

        String bio = profile.getBio();

        bioArea.setText(
            bio == null ? "" : bio
        );

        selectedPicturePath = profile.getProfilePicture();

        if (selectedPicturePath != null &&
                !selectedPicturePath.isBlank()) {

            showPicture(selectedPicturePath);
        }
    }

    // CHOOSE PROFILE PICTURE

    private void choosePicture() {

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle(
            "Choose ConnectX Profile Picture"
        );

        chooser.setFileFilter(
            new FileNameExtensionFilter(
                "Image Files (JPG, JPEG, PNG)",
                "jpg", "jpeg", "png"
            )
        );

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            File selectedFile = chooser.getSelectedFile();

            selectedPicturePath =
                selectedFile.getAbsolutePath();

            showPicture(selectedPicturePath);
        }
    }

    // DISPLAY PROFILE PICTURE

    private void showPicture(String path) {

        try {

            BufferedImage original = ImageIO.read(
                new File(path)
            );

            if (original == null) {
                throw new Exception("Invalid image");
            }

            Image scaled = original.getScaledInstance(
                115, 115, Image.SCALE_SMOOTH
            );

            pictureLabel.setText("");
            pictureLabel.setIcon(
                new ImageIcon(scaled)
            );

        } catch (Exception e) {

            pictureLabel.setIcon(null);
            pictureLabel.setText("CX");
        }
    }

    // SAVE PROFILE

    private void saveProfile() {

        String displayName =
            displayNameField.getText().trim();

        String bio = bioArea.getText().trim();

        if (displayName.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "Display name cannot be empty.",
                "ConnectX",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        boolean updated = UserDAO.updateProfile(
            username,
            displayName,
            bio,
            selectedPicturePath
        );

        if (updated) {

            if (chatClient != null) {
                chatClient.sendProfileUpdate(username);
            }

            statusLabel.setText(
                "Profile saved successfully!"
            );

            JOptionPane.showMessageDialog(
                this,
                "Your profile has been updated successfully!",
                "ConnectX",
                JOptionPane.INFORMATION_MESSAGE
            );

        } else {

            statusLabel.setText(
                "Could not save profile."
            );

            JOptionPane.showMessageDialog(
                this,
                "Unable to update your profile. Please try again.",
                "ConnectX",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ROUNDED BORDER

    private static class RoundedBorder extends AbstractBorder {

        private final Color color;
        private final int radius;

        RoundedBorder(Color color, int radius) {

            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(
                Component c,
                Graphics g,
                int x,
                int y,
                int width,
                int height) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));

            g2.drawRoundRect(
                x, y,
                width - 1, height - 1,
                radius, radius
            );

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {

            return new Insets(8, 12, 8, 12);
        }
    }

    // GRADIENT BACKGROUND

    private static class GradientPanel extends JPanel {

        private final Color first;
        private final Color second;

        GradientPanel(Color first, Color second) {

            this.first = first;
            this.second = second;

            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setPaint(
                new GradientPaint(
                    0, 0, first,
                    getWidth(), getHeight(), second
                )
            );

            g2.fillRect(
                0, 0,
                getWidth(), getHeight()
            );

            g2.setColor(
                new Color(139, 92, 246, 20)
            );

            g2.fillOval(
                getWidth() - 180,
                40,
                240,
                240
            );

            g2.setColor(
                new Color(34, 211, 238, 15)
            );

            g2.fillOval(
                -100,
                getHeight() - 180,
                250,
                250
            );

            g2.dispose();
        }
    }
}
