
package com.connectx.app;

import com.connectx.gui.DashboardPanel;
import com.connectx.database.Database;
import com.connectx.database.UserDAO;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Main {

    private static JFrame frame;
    private static CardLayout cardLayout;
    private static JPanel cardPanel;
    private static JPanel mainPanel;

    private static final Color BG = new Color(12, 15, 30);
    private static final Color PURPLE = new Color(139, 92, 246);
    private static final Color CYAN = new Color(34, 211, 238);
    private static final Color WHITE = new Color(245, 247, 255);
    private static final Color MUTED = new Color(155, 166, 190);

    public static void main(String[] args) {

        Database.initializeDatabase();

        SwingUtilities.invokeLater(() -> {

            frame = new JFrame("ConnectX - Desktop Messenger");
            frame.setSize(1050, 700);
            frame.setMinimumSize(new Dimension(900, 650));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            SplashPanel splash = new SplashPanel();
            frame.setContentPane(splash);
            frame.setVisible(true);

            splash.startAnimation();
        });
    }

    // MAIN APPLICATION

    private static void showMainApplication() {

        mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.setBackground(BG);

        JPanel leftPanel = createBrandPanel();

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(BG);

        cardPanel.add(createLoginPanel(), "LOGIN");
        cardPanel.add(createRegisterPanel(), "REGISTER");

        mainPanel.add(leftPanel);
        mainPanel.add(cardPanel);

        frame.setContentPane(mainPanel);
        frame.revalidate();
        frame.repaint();
    }

    // ANIMATED SPLASH SCREEN

    private static class SplashPanel extends JPanel {

        private Timer animationTimer;
        private int animationFrame = 0;

        private final int totalFrames = 90;

        private final int[] particleX = {
            100, 220, 350, 470, 590, 720,
            850, 950, 160, 330, 510, 780
        };

        private final int[] particleY = {
            120, 250, 160, 370, 100, 280,
            450, 180, 510, 570, 490, 590
        };

        SplashPanel() {
            setBackground(BG);
            setDoubleBuffered(true);
        }

        void startAnimation() {

            animationTimer = new Timer(30, e -> {

                animationFrame++;
                repaint();

                if (animationFrame >= totalFrames) {

                    animationTimer.stop();
                    showMainApplication();
                }
            });

            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int width = getWidth();
            int height = getHeight();

            float progress = Math.min(
                1f,
                animationFrame / (float) totalFrames
            );

            // BACKGROUND GRADIENT

            g2.setPaint(new GradientPaint(
                0, 0,
                new Color(16, 12, 39),
                width, height,
                new Color(5, 24, 45)
            ));

            g2.fillRect(0, 0, width, height);

            // BACKGROUND GLOW

            float pulse = (float) (
                0.5 + 0.5 * Math.sin(animationFrame * 0.08)
            );

            g2.setColor(new Color(
                139, 92, 246,
                (int) (18 + pulse * 20)
            ));

            g2.fillOval(
                width / 2 - 230,
                height / 2 - 230,
                460, 460
            );

            g2.setColor(new Color(34, 211, 238, 15));
            g2.fillOval(
                width / 2 - 300,
                height / 2 - 300,
                600, 600
            );

            // FLOATING PARTICLES

            for (int i = 0; i < particleX.length; i++) {

                int x = particleX[i] * width / 1050;

                int y = particleY[i] * height / 700;

                int movement = (animationFrame * (i % 3 + 1)) % 35;

                int size = 4 + (i % 4);

                int alpha = 60 + (i * 13) % 130;

                g2.setColor(new Color(
                    i % 2 == 0 ? 139 : 34,
                    i % 2 == 0 ? 92 : 211,
                    i % 2 == 0 ? 246 : 238,
                    alpha
                ));

                g2.fillOval(
                    x,
                    y - movement,
                    size,
                    size
                );
            }

            // CONNECTING NETWORK LINES

            g2.setStroke(new BasicStroke(1.2f));

            for (int i = 0; i < particleX.length - 1; i++) {

                int x1 = particleX[i] * width / 1050;
                int y1 = particleY[i] * height / 700;

                int x2 = particleX[i + 1] * width / 1050;
                int y2 = particleY[i + 1] * height / 700;

                g2.setColor(new Color(139, 92, 246, 28));

                g2.drawLine(x1, y1, x2, y2);
            }

            // CENTER LOGO ANIMATION

            int centerX = width / 2;
            int centerY = height / 2 - 55;

            float scale = 0.75f + 0.25f *
                Math.min(1f, animationFrame / 25f);

            int logoSize = (int) (105 * scale);

            // OUTER RING

            g2.setStroke(new BasicStroke(2.5f));

            g2.setColor(new Color(139, 92, 246, 100));

            g2.drawOval(
                centerX - logoSize / 2 - 15,
                centerY - logoSize / 2 - 15,
                logoSize + 30,
                logoSize + 30
            );

            // ROTATING RING

            g2.setColor(CYAN);

            g2.setStroke(new BasicStroke(
                3f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            ));

            g2.drawArc(
                centerX - logoSize / 2 - 22,
                centerY - logoSize / 2 - 22,
                logoSize + 44,
                logoSize + 44,
                animationFrame * 5,
                100
            );

            // LOGO BACKGROUND

            GradientPaint logoGradient = new GradientPaint(
                centerX - logoSize / 2,
                centerY - logoSize / 2,
                PURPLE,
                centerX + logoSize / 2,
                centerY + logoSize / 2,
                CYAN
            );

            g2.setPaint(logoGradient);

            g2.fill(new Ellipse2D.Float(
                centerX - logoSize / 2,
                centerY - logoSize / 2,
                logoSize,
                logoSize
            ));

            // LOGO SYMBOL

            g2.setFont(new Font(
                "Segoe UI Symbol",
                Font.BOLD,
                58
            ));

            g2.setColor(WHITE);

            FontMetrics fm = g2.getFontMetrics();

            String symbol = "✦";

            g2.drawString(
                symbol,
                centerX - fm.stringWidth(symbol) / 2,
                centerY + fm.getAscent() / 3
            );

            // CONNECTX TEXT

            g2.setFont(new Font(
                "Segoe UI",
                Font.BOLD,
                48
            ));

            FontMetrics logoMetrics = g2.getFontMetrics();

            String logo = "ConnectX";

            int logoTextX =
                centerX - logoMetrics.stringWidth(logo) / 2;

            g2.setColor(WHITE);

            g2.drawString(
                logo,
                logoTextX,
                centerY + 115
            );

            // TAGLINE

            g2.setFont(new Font(
                "Segoe UI",
                Font.PLAIN,
                17
            ));

            String tagline = "Connect. Chat. Share.";

            FontMetrics tagMetrics = g2.getFontMetrics();

            g2.setColor(CYAN);

            g2.drawString(
                tagline,
                centerX - tagMetrics.stringWidth(tagline) / 2,
                centerY + 151
            );

            // LOADING TEXT

            g2.setFont(new Font(
                "Segoe UI",
                Font.PLAIN,
                12
            ));

            String loading = "PREPARING YOUR WORKSPACE";

            FontMetrics loadingMetrics = g2.getFontMetrics();

            g2.setColor(MUTED);

            g2.drawString(
                loading,
                centerX - loadingMetrics.stringWidth(loading) / 2,
                centerY + 218
            );

            // LOADING BAR

            int barWidth = 260;
            int barHeight = 5;

            int barX = centerX - barWidth / 2;
            int barY = centerY + 237;

            g2.setColor(new Color(55, 65, 95));

            g2.fillRoundRect(
                barX, barY,
                barWidth, barHeight,
                10, 10
            );

            g2.setPaint(new GradientPaint(
                barX, barY,
                PURPLE,
                barX + barWidth, barY,
                CYAN
            ));

            g2.fillRoundRect(
                barX,
                barY,
                (int) (barWidth * progress),
                barHeight,
                10, 10
            );

            // FOOTER

            g2.setFont(new Font(
                "Segoe UI",
                Font.PLAIN,
                11
            ));

            String footer = "A better way to stay connected.";

            FontMetrics footerMetrics = g2.getFontMetrics();

            g2.setColor(new Color(155, 166, 190, 160));

            g2.drawString(
                footer,
                centerX - footerMetrics.stringWidth(footer) / 2,
                height - 35
            );

            g2.dispose();
        }
    }

    // BRAND PANEL

    private static JPanel createBrandPanel() {

        GradientPanel panel = new GradientPanel(
            new Color(24, 19, 55),
            new Color(9, 25, 48)
        );

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(
            BorderFactory.createEmptyBorder(140, 55, 65, 35)
        );

        JLabel icon = new JLabel("✦");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 55));
        icon.setForeground(CYAN);

        JLabel logo = new JLabel("ConnectX");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 46));
        logo.setForeground(WHITE);

        JLabel tagline = new JLabel("Connect. Chat. Share.");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        tagline.setForeground(CYAN);

        JLabel description = new JLabel(
            "<html>Your personal space for<br>" +
            "simple, private and secure<br>" +
            "communication.</html>"
        );

        description.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        description.setForeground(MUTED);

        JLabel bottom = new JLabel(
            "A better way to stay connected."
        );

        bottom.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        bottom.setForeground(MUTED);

        icon.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottom.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(icon);
        panel.add(Box.createVerticalStrut(12));
        panel.add(logo);
        panel.add(Box.createVerticalStrut(12));
        panel.add(tagline);
        panel.add(Box.createVerticalStrut(28));
        panel.add(description);
        panel.add(Box.createVerticalGlue());
        panel.add(bottom);

        return panel;
    }

    // LOGIN PANEL

    private static JPanel createLoginPanel() {

        JPanel panel = createFormPanel();

        JLabel heading = createHeading("Welcome Back!");
        JLabel subtitle = createSubtitle(
            "Sign in to continue your conversations."
        );

        JLabel usernameLabel = createLabel("Username");
        JTextField usernameField = createTextField();

        JLabel passwordLabel = createLabel("Password");
        JPasswordField passwordField = createPasswordField();

        JButton loginButton = createPrimaryButton("LOGIN");
        JButton registerButton = createSecondaryButton("CREATE ACCOUNT");

        loginButton.addActionListener(e -> {

            String username = usernameField.getText().trim();

            String password = new String(
                passwordField.getPassword()
            );

            if (username.isEmpty() || password.isEmpty()) {

                showMessage("Please enter username and password.");

            } else if (UserDAO.loginUser(username, password)) {

                JOptionPane.showMessageDialog(
                    frame,
                    "Login successful!",
                    "ConnectX",
                    JOptionPane.INFORMATION_MESSAGE
                );

                DashboardPanel dashboard = new DashboardPanel(
                    frame,
                    username,
                    () -> {
                        frame.setContentPane(mainPanel);
                        cardLayout.show(cardPanel, "LOGIN");
                        frame.revalidate();
                        frame.repaint();
                    }
                );

                frame.setContentPane(dashboard);
                frame.revalidate();
                frame.repaint();

            } else {

                showMessage("Invalid username or password.");
            }
        });

        registerButton.addActionListener(e ->
            cardLayout.show(cardPanel, "REGISTER")
        );

        passwordField.addActionListener(e ->
            loginButton.doClick()
        );

        addComponent(panel, heading);
        addGap(panel, 10);
        addComponent(panel, subtitle);
        addGap(panel, 38);
        addComponent(panel, usernameLabel);
        addGap(panel, 9);
        addComponent(panel, usernameField);
        addGap(panel, 22);
        addComponent(panel, passwordLabel);
        addGap(panel, 9);
        addComponent(panel, passwordField);
        addGap(panel, 32);
        addComponent(panel, loginButton);
        addGap(panel, 15);
        addComponent(panel, registerButton);

        return panel;
    }

    // REGISTRATION PANEL

    private static JPanel createRegisterPanel() {

        JPanel panel = createFormPanel();

        JLabel heading = createHeading("Create Account");
        JLabel subtitle = createSubtitle(
            "Join ConnectX and start connecting."
        );

        JLabel usernameLabel = createLabel("Username");
        JTextField usernameField = createTextField();

        JLabel emailLabel = createLabel("Email Address");
        JTextField emailField = createTextField();

        JLabel passwordLabel = createLabel("Password");
        JPasswordField passwordField = createPasswordField();

        JLabel confirmLabel = createLabel("Confirm Password");
        JPasswordField confirmField = createPasswordField();

        JButton createButton = createPrimaryButton("CREATE ACCOUNT");
        JButton backButton = createSecondaryButton("BACK TO LOGIN");

        createButton.addActionListener(e -> {

            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();

            String password = new String(
                passwordField.getPassword()
            );

            String confirmPassword = new String(
                confirmField.getPassword()
            );

            if (username.isEmpty() || email.isEmpty()
                    || password.isEmpty()
                    || confirmPassword.isEmpty()) {

                showMessage("Please fill in all fields.");

            } else if (!email.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

                showMessage("Please enter a valid email address.");

            } else if (password.length() < 8) {

                showMessage(
                    "Password must contain at least 8 characters."
                );

            } else if (!password.equals(confirmPassword)) {

                showMessage("Passwords do not match.");

            } else {

                String result = UserDAO.registerUser(
                    username,
                    email,
                    password
                );

                if (result.equals("SUCCESS")) {

                    showMessage(
                        "Account created successfully! Please login."
                    );

                    usernameField.setText("");
                    emailField.setText("");
                    passwordField.setText("");
                    confirmField.setText("");

                    cardLayout.show(cardPanel, "LOGIN");

                } else {

                    showMessage(result);
                }
            }
        });

        backButton.addActionListener(e ->
            cardLayout.show(cardPanel, "LOGIN")
        );

        addComponent(panel, heading);
        addGap(panel, 7);
        addComponent(panel, subtitle);
        addGap(panel, 18);
        addComponent(panel, usernameLabel);
        addGap(panel, 5);
        addComponent(panel, usernameField);
        addGap(panel, 12);
        addComponent(panel, emailLabel);
        addGap(panel, 5);
        addComponent(panel, emailField);
        addGap(panel, 12);
        addComponent(panel, passwordLabel);
        addGap(panel, 5);
        addComponent(panel, passwordField);
        addGap(panel, 12);
        addComponent(panel, confirmLabel);
        addGap(panel, 5);
        addComponent(panel, confirmField);
        addGap(panel, 20);
        addComponent(panel, createButton);
        addGap(panel, 10);
        addComponent(panel, backButton);

        return panel;
    }

    // FORM DESIGN

    private static JPanel createFormPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);

        panel.setBorder(
            BorderFactory.createEmptyBorder(55, 55, 40, 55)
        );

        return panel;
    }

    private static void addComponent(
            JPanel panel, JComponent component) {

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(component);
    }

    private static void addGap(JPanel panel, int height) {
        panel.add(Box.createVerticalStrut(height));
    }

    // LABELS

    private static JLabel createHeading(String text) {

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 31));
        label.setForeground(WHITE);
        return label;
    }

    private static JLabel createSubtitle(String text) {

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(MUTED);
        return label;
    }

    private static JLabel createLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(WHITE);
        return label;
    }

    // INPUT FIELDS

    private static JTextField createTextField() {

        JTextField field = new JTextField();
        styleField(field);
        return field;
    }

    private static JPasswordField createPasswordField() {

        JPasswordField field = new JPasswordField();
        styleField(field);
        return field;
    }

    private static void styleField(JTextField field) {

        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(WHITE);
        field.setCaretColor(CYAN);
        field.setBackground(new Color(26, 32, 55));
        field.setOpaque(true);

        field.setHorizontalAlignment(JTextField.LEFT);

        field.setBorder(
            BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(65, 77, 110), 15),
                BorderFactory.createEmptyBorder(6, 0, 6, 14)
            )
        );

        field.setPreferredSize(new Dimension(350, 44));
        field.setMaximumSize(new Dimension(350, 44));
        field.setMinimumSize(new Dimension(150, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // PRIMARY BUTTON

    private static JButton createPrimaryButton(String text) {

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
                    0, 0, getWidth(), getHeight(), 16, 16
                );

                g2.dispose();

                super.paintComponent(g);
            }
        };

        styleButton(button, WHITE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);

        return button;
    }

    // SECONDARY BUTTON

    private static JButton createSecondaryButton(String text) {

        JButton button = new JButton(text);

        styleButton(button, CYAN);
        button.setBackground(new Color(25, 35, 58));
        button.setOpaque(true);

        button.setBorder(
            new RoundedBorder(new Color(75, 105, 150), 15)
        );

        return button;
    }

    private static void styleButton(
            JButton button, Color textColor) {

        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(textColor);
        button.setFocusPainted(false);

        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        button.setPreferredSize(new Dimension(350, 46));
        button.setMaximumSize(new Dimension(350, 46));
        button.setMinimumSize(new Dimension(150, 46));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
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
                Component c, Graphics g,
                int x, int y, int width, int height) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));

            g2.drawRoundRect(
                x, y, width - 1, height - 1, radius, radius
            );

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 4, 2, 12);
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

            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(139, 92, 246, 25));
            g2.fillOval(getWidth() - 180, 40, 250, 250);

            g2.setColor(new Color(34, 211, 238, 18));
            g2.fillOval(-100, getHeight() - 180, 260, 260);

            g2.dispose();
        }
    }

    // MESSAGE DIALOG

    private static void showMessage(String message) {

        JOptionPane.showMessageDialog(
            frame,
            message,
            "ConnectX",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
}
