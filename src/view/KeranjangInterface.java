package view;

import dao.CartDAO;
import dao.OrderDAO;
import util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KeranjangInterface extends JFrame {
    private JTable cartTable;
    private DefaultTableModel tableModel;
    private int userId;

    private JButton deleteButton;
    private JButton checkoutButton;
    private JButton backButton;
    private JLabel titleLabel;

    public KeranjangInterface(int userId) {
        this.userId = userId;

        setTitle("Keranjang Belanja");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(920, 680);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.BACKGROUND);
        root.setBorder(new javax.swing.border.EmptyBorder(24, 24, 24, 24));

        // ================= HEADER =================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        titleLabel = UITheme.heading("Keranjang Belanja");
        JLabel subtitle = UITheme.muted("Tinjau kembali item sebelum checkout");
        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerText.add(titleLabel);
        headerText.add(Box.createRigidArea(new Dimension(0, 4)));
        headerText.add(subtitle);
        headerPanel.add(headerText, BorderLayout.WEST);
        root.add(headerPanel, BorderLayout.NORTH);

        // ================= TABEL =================
        tableModel = new DefaultTableModel(
                new Object[]{"Product ID", "Nama Produk", "Harga", "Jumlah", "Subtotal"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: return Integer.class;
                    case 1: return String.class;
                    case 2: return Double.class;
                    case 3: return Integer.class;
                    case 4: return Double.class;
                    default: return Object.class;
                }
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cartTable = new JTable(tableModel);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(cartTable);

        // Renderer mata uang (Harga & Subtotal) dengan warna baris bergantian
        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Object display = (value instanceof Number)
                        ? "Rp " + String.format("%,.0f", ((Number) value).doubleValue())
                        : value;
                super.getTableCellRendererComponent(t, display, isSelected, hasFocus, row, column);
                setBackground(isSelected ? UITheme.SELECTION
                        : (row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT));
                setForeground(UITheme.TEXT);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(new javax.swing.border.EmptyBorder(0, 12, 0, 12));
                return this;
            }
        };
        cartTable.getColumnModel().getColumn(2).setCellRenderer(moneyRenderer);
        cartTable.getColumnModel().getColumn(4).setCellRenderer(moneyRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setBackground(isSelected ? UITheme.SELECTION
                        : (row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT));
                setForeground(UITheme.TEXT);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new javax.swing.border.EmptyBorder(0, 12, 0, 12));
                return this;
            }
        };
        cartTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Sembunyikan kolom product_id di tampilan
        cartTable.getColumnModel().getColumn(0).setMinWidth(0);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(0);
        cartTable.getColumnModel().getColumn(0).setWidth(0);

        cartTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(130);

        JPanel tableCard = UITheme.card(new BorderLayout());
        tableCard.add(UITheme.scrollPane(cartTable), BorderLayout.CENTER);
        root.add(tableCard, BorderLayout.CENTER);

        // ================= FOOTER =================
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        backButton = UITheme.neutralButton("← Kembali");
        backButton.addActionListener(e -> backToCustomerInterface());

        deleteButton = UITheme.dangerButton("Hapus Produk");
        deleteButton.addActionListener(e -> deleteSelectedItem());

        checkoutButton = UITheme.successButton("Checkout");
        checkoutButton.addActionListener(e -> checkoutCart());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(deleteButton);
        rightPanel.add(checkoutButton);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(backButton);

        bottomPanel.add(leftPanel, BorderLayout.WEST);
        bottomPanel.add(rightPanel, BorderLayout.EAST);
        root.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(root);

        loadCartItems();
        setVisible(true);
    }

    /**
     * Kembali ke CustomerInterface
     */
    private void backToCustomerInterface() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            try {
                new CustomerInterface(userId);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Gagal membuka halaman utama: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Muat data dari tabel shopping_cart dan products, berdasarkan userId.
     */
    private void loadCartItems() {
        tableModel.setRowCount(0);
        try {
            for (Object[] row : CartDAO.getCartRows(userId)) {
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat isi keranjang: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        updateTotalDisplay();
    }

    private void updateTotalDisplay() {
        double total = 0.0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object subtotalObj = tableModel.getValueAt(i, 4);
            if (subtotalObj instanceof Double) {
                total += (Double) subtotalObj;
            }
        }
        titleLabel.setText(String.format("Keranjang Belanja - Total: Rp %,.0f", total));
    }

    /**
     * Hapus item yang dipilih dari shopping_cart, lalu refresh tabel.
     */
    private void deleteSelectedItem() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang ingin dihapus.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int productId = (Integer) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Yakin ingin menghapus produk dari keranjang?",
                "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            CartDAO.removeByProduct(userId, productId);
            JOptionPane.showMessageDialog(this, "Produk berhasil dihapus dari keranjang.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            loadCartItems();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menghapus produk: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Proses checkout: baca ulang stok & harga dari DB, periksa stok, insert order & item, update stok,
     * insert sales_history, kosongkan shopping_cart. Semua dalam satu transaksi.
     */
    private void checkoutCart() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "Keranjang kosong.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double totalAmount = 0.0;
        for (int i = 0; i < rowCount; i++) {
            totalAmount += (Double) tableModel.getValueAt(i, 4);
        }

        String message = String.format(
                "<html><div style='text-align: center; font-family: Segoe UI;'>" +
                        "<h3 style='color: #1E293B; margin: 10px 0;'>Konfirmasi Checkout</h3>" +
                        "<p style='font-size: 14px; color: #64748B;'>Total pembayaran:</p>" +
                        "<p style='font-size: 18px; font-weight: bold; color: #16A34A;'>Rp %,.0f</p>" +
                        "<p style='font-size: 12px; color: #94A3B8;'>Lanjutkan checkout?</p>" +
                        "</div></html>", totalAmount);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                message,
                "Konfirmasi Checkout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int orderId = OrderDAO.checkout(userId);

            String successMessage =
                    "<html><div style='text-align: center; font-family: Segoe UI;'>" +
                            "<h3 style='color: #16A34A; margin: 10px 0;'>Checkout Berhasil!</h3>" +
                            "<p style='font-size: 14px; color: #1E293B;'>Terima kasih telah berbelanja.</p>" +
                            "<p style='font-size: 12px; color: #64748B;'>Order ID: " + orderId + "</p>" +
                            "</div></html>";

            JOptionPane.showMessageDialog(this, successMessage, "Sukses", JOptionPane.INFORMATION_MESSAGE);
            loadCartItems();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal saat checkout: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        UITheme.install();
        SwingUtilities.invokeLater(() -> new KeranjangInterface(2));
    }
}
