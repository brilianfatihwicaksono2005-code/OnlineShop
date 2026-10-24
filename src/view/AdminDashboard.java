package view;

import dao.ActivityLogDAO;
import dao.ProductDAO;
import dao.UserDAO;
import model.Session;
import util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.*;
import java.sql.*;

public class AdminDashboard extends JFrame {

    private JTextField nameField, priceField, stockField, categoryField;
    private JTextArea descriptionArea;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton addButton, updateButton, deleteButton, uploadImageButton;
    private JLabel imageLabel, imagePreview;
    private String imagePath = null;

    private AnalyticsPanel analyticsPanel;
    private OrderManagementPanel orderPanel;

    public AdminDashboard() {
        setTitle("Admin Dashboard - Online Shop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 860);
        setMinimumSize(new Dimension(980, 700));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(UITheme.BACKGROUND);
        root.setBorder(new javax.swing.border.EmptyBorder(24, 24, 24, 24));

        // ================= HEADER =================
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = UITheme.title("Dashboard Admin");
        JLabel subtitle = UITheme.muted("Kelola katalog produk toko online Anda");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        // ================= FORM + TABLE =================
        JPanel formCard = UITheme.card(new BorderLayout(24, 0));

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);

        nameField = UITheme.textField(20);
        priceField = UITheme.textField(20);
        stockField = UITheme.textField(20);
        categoryField = UITheme.textField(20);
        descriptionArea = UITheme.textArea(4, 20);

        addFormRow(formGrid, 0, "Nama Produk", nameField);
        addFormRow(formGrid, 1, "Harga", priceField);
        addFormRow(formGrid, 2, "Stok", stockField);
        addFormRow(formGrid, 3, "Kategori", categoryField);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 0, 6, 16);
        formGrid.add(UITheme.label("Deskripsi"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(10, 0, 6, 0);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(new javax.swing.border.LineBorder(UITheme.BORDER, 1, true));
        formGrid.add(descScroll, gbc);

        // Panel gambar
        JPanel imagePanel = new JPanel();
        imagePanel.setOpaque(false);
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));

        JLabel imageTitle = UITheme.label("Gambar Produk");
        imageTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        imagePreview = new JLabel();
        imagePreview.setPreferredSize(new Dimension(170, 170));
        imagePreview.setMaximumSize(new Dimension(170, 170));
        imagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        imagePreview.setOpaque(true);
        imagePreview.setBackground(UITheme.BACKGROUND);
        imagePreview.setBorder(new javax.swing.border.LineBorder(UITheme.BORDER, 1, true));
        imagePreview.setAlignmentX(Component.CENTER_ALIGNMENT);

        imageLabel = new JLabel("Belum ada gambar");
        imageLabel.setFont(UITheme.FONT_SMALL);
        imageLabel.setForeground(UITheme.TEXT_MUTED);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        uploadImageButton = UITheme.warningButton("Upload Gambar");
        uploadImageButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadImageButton.setBorder(new javax.swing.border.EmptyBorder(9, 18, 9, 18));

        imagePanel.add(imageTitle);
        imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        imagePanel.add(imagePreview);
        imagePanel.add(Box.createRigidArea(new Dimension(0, 8)));
        imagePanel.add(imageLabel);
        imagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        imagePanel.add(uploadImageButton);

        formCard.add(formGrid, BorderLayout.CENTER);
        formCard.add(imagePanel, BorderLayout.EAST);

        // Tombol aksi
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);
        addButton = UITheme.primaryButton("Tambah Produk");
        updateButton = UITheme.successButton("Update Produk");
        deleteButton = UITheme.dangerButton("Hapus Produk");
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        JPanel topPanel = new JPanel(new BorderLayout(0, 16));
        topPanel.setOpaque(false);
        topPanel.add(formCard, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Tabel
        JPanel tableCard = UITheme.card(new BorderLayout(0, 14));
        tableCard.add(UITheme.heading("Daftar Produk"), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Nama", "Harga", "Stok", "Kategori", "Deskripsi", "Gambar"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);

        // Renderer mata uang untuk kolom Harga
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Object display = (value instanceof Number)
                        ? String.format("Rp %,.0f", ((Number) value).doubleValue())
                        : value;
                super.getTableCellRendererComponent(t, display, isSelected, hasFocus, row, column);
                setBackground(isSelected ? UITheme.SELECTION
                        : (row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT));
                setForeground(UITheme.TEXT);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(new javax.swing.border.EmptyBorder(0, 12, 0, 12));
                return this;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(260);
        table.getColumnModel().getColumn(6).setPreferredWidth(140);

        tableCard.add(UITheme.scrollPane(table), BorderLayout.CENTER);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.weightx = 1;
        cc.fill = GridBagConstraints.BOTH;
        cc.insets = new Insets(0, 0, 16, 0);
        cc.gridy = 0;
        cc.weighty = 0.62;
        content.add(topPanel, cc);
        cc.gridy = 1;
        cc.weighty = 0.38;
        cc.insets = new Insets(0, 0, 0, 0);
        content.add(tableCard, cc);

        root.add(content, BorderLayout.CENTER);

        // ================= TABBED SHELL: Analitik + Produk + Pesanan =================
        analyticsPanel = new AnalyticsPanel();
        orderPanel = new OrderManagementPanel();
        ActivityLogPanel logPanel = new ActivityLogPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BOLD);
        tabs.addTab("  Dashboard Analitik  ", analyticsPanel);
        tabs.addTab("  Kelola Produk  ", root);
        tabs.addTab("  Kelola Pesanan  ", orderPanel);
        tabs.addTab("  Log Aktivitas  ", logPanel);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == analyticsPanel) {
                analyticsPanel.refresh();
            } else if (tabs.getSelectedComponent() == orderPanel) {
                orderPanel.loadOrders();
            } else if (tabs.getSelectedComponent() == logPanel) {
                logPanel.refresh();
            }
        });
        setContentPane(tabs);

        loadProducts();

        uploadImageButton.addActionListener(e -> uploadImage());
        addButton.addActionListener(e -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                nameField.setText((String) tableModel.getValueAt(row, 1));
                priceField.setText(tableModel.getValueAt(row, 2).toString());
                stockField.setText(tableModel.getValueAt(row, 3).toString());
                categoryField.setText((String) tableModel.getValueAt(row, 4));
                descriptionArea.setText((String) tableModel.getValueAt(row, 5));
                String img = (String) tableModel.getValueAt(row, 6);
                imageLabel.setText(img);
                imagePath = "images/" + img;
                showImagePreview(imagePath);
            }
        });

        setVisible(true);
    }

    private void addFormRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 14, 16);
        panel.add(UITheme.label(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);
        panel.add(field, gbc);
    }

    private void uploadImage() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            String targetDir = "images/";
            File dir = new File(targetDir);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(targetDir + selectedFile.getName());
            try {
                if (dest.exists()) dest.setWritable(true);
                Files.copy(selectedFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                imagePath = dest.getPath();
                imageLabel.setText(selectedFile.getName());
                showImagePreview(imagePath);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Gagal mengunggah gambar.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showImagePreview(String path) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(170, 170, Image.SCALE_SMOOTH);
        imagePreview.setIcon(new ImageIcon(img));
    }

    private String adminName() {
        try {
            return UserDAO.getUsernameById(Session.getCurrentUserId());
        } catch (SQLException e) {
            return "admin";
        }
    }

    private void loadProducts() {
        tableModel.setRowCount(0);
        try {
            for (Object[] row : ProductDAO.getAllRows()) {
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addProduct() {
        if (!validateForm()) return;
        try {
            ProductDAO.insert(nameField.getText(), Double.parseDouble(priceField.getText()),
                    Integer.parseInt(stockField.getText()), categoryField.getText(),
                    descriptionArea.getText(), imageLabel.getText());
            ActivityLogDAO.log(adminName(), "Menambah produk: " + nameField.getText());
            loadProducts();
            resetForm();
            JOptionPane.showMessageDialog(this, "Produk berhasil ditambahkan.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menambah produk.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang ingin diupdate.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!validateForm()) return;
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            ProductDAO.update(id, nameField.getText(), Double.parseDouble(priceField.getText()),
                    Integer.parseInt(stockField.getText()), categoryField.getText(),
                    descriptionArea.getText(), imageLabel.getText());
            ActivityLogDAO.log(adminName(), "Mengubah produk #" + id + " (" + nameField.getText() + ")");
            loadProducts();
            resetForm();
            JOptionPane.showMessageDialog(this, "Produk berhasil diperbarui.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal mengupdate produk.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang ingin dihapus.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String name = String.valueOf(tableModel.getValueAt(row, 1));

        try {
            if (ProductDAO.countOrderItems(id) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Produk tidak dapat dihapus karena sudah ada dalam riwayat pesanan.",
                        "Gagal Hapus", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error saat cek referensi pesanan: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Yakin ingin menghapus produk ini?",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            ProductDAO.delete(id);
            ActivityLogDAO.log(adminName(), "Menghapus produk #" + id + " (" + name + ")");
            loadProducts();
            resetForm();
            JOptionPane.showMessageDialog(this, "Produk berhasil dihapus.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal menghapus produk: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateForm() {
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();
        String stockText = stockField.getText().trim();
        String category = categoryField.getText().trim();
        String description = descriptionArea.getText().trim();
        String imageText = imageLabel.getText();

        if (name.isEmpty() || priceText.isEmpty() || stockText.isEmpty() ||
                category.isEmpty() || description.isEmpty() ||
                "Belum ada gambar".equals(imageText)) {
            JOptionPane.showMessageDialog(this, "Semua field harus diisi dan gambar harus di-upload.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "Harga harus lebih dari 0.", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Harga harus berupa angka.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            int stock = Integer.parseInt(stockText);
            if (stock < 0) {
                JOptionPane.showMessageDialog(this, "Stok tidak boleh negatif.", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Stok harus berupa bilangan bulat.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }

    private void resetForm() {
        nameField.setText("");
        priceField.setText("");
        stockField.setText("");
        categoryField.setText("");
        descriptionArea.setText("");
        imageLabel.setText("Belum ada gambar");
        imagePreview.setIcon(null);
        imagePath = null;
    }

    public static void main(String[] args) {
        UITheme.install();
        SwingUtilities.invokeLater(Login::new);
    }
}
