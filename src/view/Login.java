package view;

import dao.ActivityLogDAO;
import dao.UserDAO;
import model.Session;
import util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Login extends JFrame implements ActionListener {

    private JTextField inputUser;
    private JPasswordField inputPassword;
    private JButton submitButton;
    private JButton cancelButton;
    private JLabel messageLabel;

    public Login() {
        setTitle("Online Shop - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 640);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);

        // Root: background seragam + padding luas
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(UITheme.BACKGROUND);
        root.setBorder(new javax.swing.border.EmptyBorder(40, 40, 40, 40));

        // Card login
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.SURFACE);
        card.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.LineBorder(UITheme.BORDER, 1, true),
                new javax.swing.border.EmptyBorder(36, 40, 36, 40)));
        card.setPreferredSize(new Dimension(400, 566));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Logo
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(createLogo(), gbc);

        // Judul & subjudul
        JLabel title = UITheme.centeredTitle("Online Shop System");
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(18, 0, 0, 0);
        card.add(title, gbc);

        JLabel subtitle = UITheme.muted("Masuk untuk melanjutkan");
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 2;
        gbc.insets = new Insets(4, 0, 0, 0);
        card.add(subtitle, gbc);

        // Username
        JLabel userLabel = UITheme.label("Username");
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(26, 0, 6, 0);
        card.add(userLabel, gbc);

        inputUser = UITheme.textField(18);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(inputUser, gbc);

        // Password
        JLabel passLabel = UITheme.label("Password");
        gbc.gridy = 5;
        gbc.insets = new Insets(14, 0, 6, 0);
        card.add(passLabel, gbc);

        inputPassword = UITheme.passwordField(18);
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(inputPassword, gbc);

        // Pesan
        messageLabel = new JLabel(" ");
        messageLabel.setFont(UITheme.FONT_SMALL);
        messageLabel.setForeground(UITheme.DANGER);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 7;
        gbc.insets = new Insets(12, 0, 0, 0);
        card.add(messageLabel, gbc);

        // Tombol
        submitButton = UITheme.primaryButton("Login");
        submitButton.setBorder(new javax.swing.border.EmptyBorder(12, 20, 12, 20));
        gbc.gridy = 8;
        gbc.insets = new Insets(6, 0, 0, 0);
        card.add(submitButton, gbc);

        cancelButton = UITheme.neutralButton("Cancel");
        cancelButton.setBorder(new javax.swing.border.EmptyBorder(12, 20, 12, 20));
        gbc.gridy = 9;
        gbc.insets = new Insets(10, 0, 0, 0);
        card.add(cancelButton, gbc);

        root.add(card);
        setContentPane(root);

        submitButton.addActionListener(this);
        cancelButton.addActionListener(this);

        getRootPane().setDefaultButton(submitButton);
        setVisible(true);
    }

    /** Logo bulat dengan inisial aplikasi. */
    private JComponent createLogo() {
        JComponent logo = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                String t = "OS";
                int x = (getWidth() - fm.stringWidth(t)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(t, x, y);
                g2.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(64, 64));
        logo.setMinimumSize(new Dimension(64, 64));
        logo.setMaximumSize(new Dimension(64, 64));
        return logo;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == submitButton) {
            String username = inputUser.getText();
            String password = String.valueOf(inputPassword.getPassword());

            try {
                UserDAO.User user = UserDAO.authenticate(username, password);
                if (user != null) {
                    messageLabel.setForeground(UITheme.SUCCESS);
                    messageLabel.setText("Login berhasil sebagai " + user.role + "!");

                    Session.setCurrentUserId(user.id);

                    if (user.isAdmin()) {
                        ActivityLogDAO.log(user.username, "Login sukses sebagai admin");
                    }

                    dispose();

                    if (user.isAdmin()) {
                        new AdminDashboard();
                    } else if ("customer".equalsIgnoreCase(user.role)) {
                        new CustomerInterface(user.id);
                    }
                } else {
                    messageLabel.setForeground(UITheme.DANGER);
                    messageLabel.setText("Username atau password salah!");
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                messageLabel.setForeground(UITheme.DANGER);
                messageLabel.setText("Terjadi kesalahan koneksi.");
            }

        } else if (e.getSource() == cancelButton) {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        UITheme.install();
        SwingUtilities.invokeLater(Login::new);
    }
}
