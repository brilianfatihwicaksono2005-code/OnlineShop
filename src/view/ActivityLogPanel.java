package view;

import dao.ActivityLogDAO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
import java.sql.SQLException;

/**
 * ActivityLogPanel - menampilkan audit trail aktivitas admin dari tabel activity_logs.
 */
public class ActivityLogPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public ActivityLogPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Log Aktivitas Admin");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Waktu", "User", "Aktivitas"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(520);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        JButton refresh = new JButton("Muat Ulang");
        refresh.addActionListener(e -> refresh());
        bottom.add(refresh);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        model.setRowCount(0);
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        try {
            for (Object[] row : ActivityLogDAO.getAll()) {
                model.addRow(new Object[]{
                        fmt.format((java.sql.Timestamp) row[3]),
                        row[1],
                        row[2]
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}