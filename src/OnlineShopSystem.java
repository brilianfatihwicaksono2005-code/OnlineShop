import util.UITheme;
import view.Login;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class OnlineShopSystem extends JFrame implements ActionListener {

    private JButton btnManageProducts, btnManageOrders, btnLogout;

    public OnlineShopSystem() {
        setTitle("Admin Dashboard - Online Shop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 640);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(UITheme.BACKGROUND);
        root.setBorder(new javax.swing.border.EmptyBorder(40, 40, 40, 40));

        JPanel card = UITheme.card(new GridBagLayout());
        card.setPreferredSize(new Dimension(380, 440));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);

        JLabel title = UITheme.centeredTitle("Admin Panel");
        gbc.gridy = 0;
        card.add(title, gbc);

        JLabel subtitle = UITheme.muted("Kelola toko online Anda");
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 22, 0);
        card.add(subtitle, gbc);

        btnManageProducts = UITheme.primaryButton("Kelola Produk");
        btnManageProducts.setBorder(new javax.swing.border.EmptyBorder(12, 20, 12, 20));
        btnManageProducts.addActionListener(this);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(btnManageProducts, gbc);

        btnManageOrders = UITheme.neutralButton("Kelola Pesanan");
        btnManageOrders.setBorder(new javax.swing.border.EmptyBorder(12, 20, 12, 20));
        btnManageOrders.addActionListener(this);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(btnManageOrders, gbc);

        btnLogout = UITheme.dangerButton("Logout");
        btnLogout.setBorder(new javax.swing.border.EmptyBorder(12, 20, 12, 20));
        btnLogout.addActionListener(this);
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 0, 0);
        card.add(btnLogout, gbc);

        root.add(card);
        setContentPane(root);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnManageProducts) {
            JOptionPane.showMessageDialog(this, "Fitur kelola produk belum tersedia.");
        } else if (e.getSource() == btnManageOrders) {
            JOptionPane.showMessageDialog(this, "Fitur kelola pesanan belum tersedia.");
        } else if (e.getSource() == btnLogout) {
            dispose();
            new Login();
        }
    }

    public static void main(String[] args) {
        UITheme.install();
        SwingUtilities.invokeLater(OnlineShopSystem::new);
    }
}
