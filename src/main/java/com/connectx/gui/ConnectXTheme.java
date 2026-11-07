
package com.connectx.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

public final class ConnectXTheme {

    private ConnectXTheme() {
    }

    // Midnight Aurora colour palette
    public static final Color BACKGROUND = new Color(9, 11, 22);
    public static final Color SIDEBAR = new Color(21, 26, 46);
    public static final Color PANEL = new Color(37, 43, 69);

    public static final Color PURPLE = new Color(139, 92, 246);
    public static final Color CYAN = new Color(34, 211, 238);

    public static final Color TEXT = new Color(248, 250, 252);
    public static final Color MUTED = new Color(148, 163, 184);
    public static final Color BORDER = new Color(55, 65, 95);

    // Poppins fonts
    public static final Font REGULAR = loadFont(
            "assets/fonts/Poppins-Regular.ttf",
            Font.PLAIN,
            14
    );

    public static final Font SEMIBOLD = loadFont(
            "assets/fonts/Poppins-SemiBold.ttf",
            Font.BOLD,
            14
    );

    private static Font loadFont(String path, int style, float size) {
        try {
            File fontFile = new File(path);

            if (fontFile.exists()) {
                Font font = Font.createFont(
                        Font.TRUETYPE_FONT,
                        fontFile
                );

                return font.deriveFont(style, size);
            }

            System.out.println("Font not found: " + path);

        } catch (Exception e) {
            System.out.println("Could not load font: " + path);
            e.printStackTrace();
        }

        return new Font("SansSerif", style, (int) size);
    }

    public static Font regular(float size) {
        return REGULAR.deriveFont(size);
    }

    public static Font semibold(float size) {
        return SEMIBOLD.deriveFont(size);
    }

    // Rounded panel
    public static class RoundedPanel extends JPanel {

        private final int radius;
        private final Color panelColor;

        public RoundedPanel(Color color, int radius) {
            this.panelColor = color;
            this.radius = radius;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(panelColor);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    radius,
                    radius
            );

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    // Modern rounded button
    public static class AuroraButton extends JButton {

        private boolean hovered = false;

        public AuroraButton(String text) {
            super(text);

            setFont(semibold(13));
            setForeground(TEXT);
            setBackground(PURPLE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            setBorder(new EmptyBorder(12, 22, 12, 22));

            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color buttonColor = hovered
                    ? new Color(155, 112, 255)
                    : PURPLE;

            if (!isEnabled()) {
                buttonColor = BORDER;
            }

            g2.setColor(buttonColor);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    16,
                    16
            );

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    // Modern text field
    public static JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();

        field.setFont(regular(13));
        field.setForeground(TEXT);
        field.setBackground(PANEL);
        field.setCaretColor(CYAN);

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        field.setToolTipText(placeholder);

        return field;
    }

    // Common label
    public static JLabel createLabel(String text, float size) {
        JLabel label = new JLabel(text);

        label.setFont(regular(size));
        label.setForeground(TEXT);

        return label;
    }

    // Heading label
    public static JLabel createHeading(String text, float size) {
        JLabel label = new JLabel(text);

        label.setFont(semibold(size));
        label.setForeground(TEXT);

        return label;
    }
}
