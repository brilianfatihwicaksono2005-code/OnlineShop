package view;

import dao.ActivityLogDAO;
import dao.OrderDAO;
import dao.UserDAO;
import model.OrderStatus;
import model.Session;
import util.ReportExporter;
import util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OrderManagementPanel - antarmuka admin untuk mengelola status pesanan
 * mengikuti state machine: Pending -> Diproses -> Dikirim -> Selesai
 * (dengan opsi pembatalan saat masih Pending).
 */
public class OrderManagementPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> filterCombo;
    private final JButton advanceButton;
    private final JButton cancelButton;
    private final JButton invoiceButton;
    private final JLabel infoLabel;

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMM yyyy, HH:mm");

    public OrderManagementPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        // ---------------- HEADER ----------------
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = UITheme.heading("Kelola Pesanan");
        JLabel subtitle = UITheme.muted("Perbarui status pesanan sesuai alur: Pending → Diproses → Dikirim → Selesai");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterBox.setOpaque(false);
        filterBox.add(UITheme.muted("Status:"));
        filterCombo = new JComboBox<>(new String[]{"Semua", "Pending", "Diproses", "Dikirim", "Selesai", "Dibatalkan"});
        filterCombo.setFont(UITheme.FONT_BASE);
        filterCombo.setBackground(UITheme.SURFACE);
        filterCombo.addActionListener(e -> loadOrders());
        filterBox.add(filterCombo);
        header.add(filterBox, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ---------------- TABLE ----------------
        tableModel = new DefaultTableModel(
                new Object[]{"ID Pesanan", "Pelanggan", "Tanggal", "Total", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setBackground(isSelected ? UITheme.SELECTION : (row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT));
                setForeground(statusColor(String.valueOf(value)));
                setFont(UITheme.FONT_BOLD);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return this;
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(statusRenderer);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int orderId = (int) tableModel.getValueAt(table.getSelectedRow(), 0);
                    new OrderItemsDialog(SwingUtilities.getWindowAncestor(OrderManagementPanel.this), orderId);
                }
            }
        });

        JPanel tableCard = UITheme.card(new BorderLayout(0, 12));
        tableCard.add(UITheme.scrollPane(table), BorderLayout.CENTER);

        infoLabel = UITheme.muted("Klik ganda pada baris untuk melihat rincian item.");
        tableCard.add(infoLabel, BorderLayout.SOUTH);

        add(tableCard, BorderLayout.CENTER);

        // ---------------- ACTIONS ----------------
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton refreshButton = UITheme.neutralButton("Refresh");
        refreshButton.addActionListener(e -> loadOrders());

        invoiceButton = UITheme.neutralButton("Cetak Invoice");
        invoiceButton.addActionListener(e -> exportInvoice());

        cancelButton = UITheme.dangerButton("Batalkan");
        cancelButton.addActionListener(e -> cancelOrder());

        advanceButton = UITheme.successButton("Perbarui Status");
        advanceButton.addActionListener(e -> advanceStatus());

        actions.add(refreshButton);
        actions.add(invoiceButton);
        actions.add(cancelButton);
        actions.add(advanceButton);
        add(actions, BorderLayout.SOUTH);

        loadOrders();
    }

    private Color statusColor(String label) {
        switch (label) {
            case "Pending":    return UITheme.WARNING_DARK;
            case "Diproses":   return UITheme.PRIMARY_DARK;
            case "Dikirim":    return UITheme.NEUTRAL_DARK;
            case "Selesai":    return UITheme.SUCCESS_DARK;
            case "Dibatalkan": return UITheme.DANGER_DARK;
            default:           return UITheme.TEXT;
        }
    }

    private String selectedFilterDbValue() {
        int idx = filterCombo.getSelectedIndex();
        switch (idx) {
            case 1: return "pending";
            case 2: return "diproses";
            case 3: return "dikirim";
            case 4: return "selesai";
            case 5: return "cancelled";
            default: return null;
        }
    }

    public void loadOrders() {
        tableModel.setRowCount(0);
        String filter = selectedFilterDbValue();
        try {
            for (Object[] row : OrderDAO.getAllOrders(filter)) {
                Timestamp date = (Timestamp) row[2];
                OrderStatus status = OrderStatus.fromDb((String) row[4]);
                tableModel.addRow(new Object[]{
                        row[0],
                        row[1],
                        DATE_FMT.format(date),
                        CURRENCY.format((Double) row[3]),
                        status.getLabel()
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat pesanan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int selectedOrderId() {
        int row = table.getSelectedRow();
        if (row < 0) return -1;
        return (int) tableModel.getValueAt(row, 0);
    }

    private OrderStatus selectedStatus() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return OrderStatus.fromDb(dbValueOfLabel((String) tableModel.getValueAt(row, 4)));
    }

    private String dbValueOfLabel(String label) {
        for (OrderStatus s : OrderStatus.values()) {
            if (s.getLabel().equals(label)) return s.getDbValue();
        }
        return "pending";
    }

    private void advanceStatus() {
        int orderId = selectedOrderId();
        if (orderId < 0) {
            JOptionPane.showMessageDialog(this, "Pilih pesanan terlebih dahulu.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        OrderStatus current = selectedStatus();
        if (current == null) return;

        OrderStatus next = current.next();
        if (next == null || !current.canTransitionTo(next)) {
            JOptionPane.showMessageDialog(this,
                    "Pesanan sudah pada status final (" + current.getLabel() + ") dan tidak dapat dilanjutkan.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Ubah status pesanan #" + orderId + " dari \"" + current.getLabel() + "\" menjadi \"" + next.getLabel() + "\"?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        updateStatus(orderId, next);
    }

    private void cancelOrder() {
        int orderId = selectedOrderId();
        if (orderId < 0) {
            JOptionPane.showMessageDialog(this, "Pilih pesanan terlebih dahulu.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        OrderStatus current = selectedStatus();
        if (current == null) return;

        if (!current.canTransitionTo(OrderStatus.CANCELLED)) {
            JOptionPane.showMessageDialog(this,
                    "Hanya pesanan berstatus Pending yang dapat dibatalkan.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Batalkan pesanan #" + orderId + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        updateStatus(orderId, OrderStatus.CANCELLED);
    }

    private String currentAdminName() {
        try {
            return UserDAO.getUsernameById(Session.getCurrentUserId());
        } catch (SQLException e) {
            return "admin";
        }
    }

    private void updateStatus(int orderId, OrderStatus target) {
        try {
            OrderDAO.updateStatus(orderId, target);
            ActivityLogDAO.log(currentAdminName(),
                    "Mengubah status pesanan #" + orderId + " menjadi " + target.getLabel());
            JOptionPane.showMessageDialog(this, "Status pesanan diperbarui menjadi \"" + target.getLabel() + "\".");
            loadOrders();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memperbarui status: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportInvoice() {
        int orderId = selectedOrderId();
        if (orderId < 0) {
            JOptionPane.showMessageDialog(this, "Pilih pesanan terlebih dahulu.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("invoice-" + orderId + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dest = ensurePdfExtension(chooser.getSelectedFile());
        try {
            ReportExporter.exportInvoice(orderId, dest);
            JOptionPane.showMessageDialog(this, "Invoice tersimpan: " + dest.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal membuat invoice: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static File ensurePdfExtension(File f) {
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            return new File(f.getParentFile(), f.getName() + ".pdf");
        }
        return f;
    }

    // =========================================================
    // Dialog rincian item pesanan
    // =========================================================
    static class OrderItemsDialog extends JDialog {
        OrderItemsDialog(Window owner, int orderId) {
            super(owner, "Rincian Pesanan #" + orderId, Dialog.ModalityType.APPLICATION_MODAL);
            setSize(560, 400);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(UITheme.BACKGROUND);
            setLayout(new BorderLayout());
            getRootPane().setBorder(new EmptyBorder(18, 18, 18, 18));

            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Produk", "Qty", "Harga Satuan", "Subtotal"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable items = new JTable(model);
            UITheme.styleTable(items);
            add(UITheme.scrollPane(items), BorderLayout.CENTER);

            try {
                for (Object[] r : OrderDAO.getOrderItems(orderId)) {
                    model.addRow(new Object[]{
                            r[0],
                            r[1],
                            CURRENCY.format((Double) r[2]),
                            CURRENCY.format((Double) r[3])
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            setVisible(true);
        }
    }
}