package view;

import dao.OrderDAO;
import model.OrderStatus;
import util.ReportExporter;
import util.UITheme;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * AnalyticsPanel - dasbor analitik admin.
 *
 * Menampilkan metrik ringkas (Total Pendapatan, Pesanan Aktif, Produk Terlaris)
 * dan visualisasi data memakai JFreeChart (diagram batang + grafik pie).
 * Juga menyediakan ekspor rekap laporan penjualan bulanan ke PDF (iText).
 */
public class AnalyticsPanel extends JPanel {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    private final JPanel metricsRow;
    private final JPanel chartsRow;

    public AnalyticsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        // ---------------- HEADER ----------------
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = UITheme.heading("Dashboard Analitik");
        JLabel subtitle = UITheme.muted("Ringkasan performa penjualan toko Anda");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton refreshButton = UITheme.neutralButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        JButton exportButton = UITheme.primaryButton("Ekspor Laporan Bulanan (PDF)");
        exportButton.addActionListener(e -> exportMonthlyReport());
        actions.add(refreshButton);
        actions.add(exportButton);
        header.add(actions, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ---------------- BODY ----------------
        metricsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsRow.setOpaque(false);
        metricsRow.setPreferredSize(new Dimension(0, 120));

        chartsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsRow.setOpaque(false);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(metricsRow, BorderLayout.NORTH);
        body.add(chartsRow, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        refresh();
    }

    // =========================================================
    // DATA + RENDER
    // =========================================================
    public void refresh() {
        double revenue = 0;
        int activeOrders = 0;
        int totalOrders = 0;
        String bestSeller = "-";
        int bestQty = 0;

        DefaultCategoryDataset barData = new DefaultCategoryDataset();
        DefaultPieDataset<String> pieData = new DefaultPieDataset<>();

        try {
            revenue = OrderDAO.getTotalRevenue();
            activeOrders = OrderDAO.countActiveOrders();
            totalOrders = OrderDAO.countAllOrders();

            // Top 5 produk terlaris
            boolean first = true;
            for (Object[] row : OrderDAO.getTopProducts(5)) {
                String name = (String) row[0];
                int qty = (Integer) row[1];
                barData.addValue(qty, "Terjual", name);
                if (first) { bestSeller = name; bestQty = qty; first = false; }
            }

            // Distribusi status pesanan
            for (Object[] row : OrderDAO.getStatusDistribution()) {
                OrderStatus st = OrderStatus.fromDb((String) row[0]);
                pieData.setValue(st.getLabel(), (Integer) row[1]);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data analitik: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // ----- metric cards -----
        metricsRow.removeAll();
        metricsRow.add(metricCard("Total Pendapatan", CURRENCY.format(revenue), UITheme.SUCCESS));
        metricsRow.add(metricCard("Pesanan Aktif", String.valueOf(activeOrders), UITheme.WARNING));
        metricsRow.add(metricCard("Total Pesanan", String.valueOf(totalOrders), UITheme.PRIMARY));
        metricsRow.add(metricCard("Produk Terlaris", bestSeller.equals("-") ? "-" : bestSeller + " (" + bestQty + ")", UITheme.NEUTRAL));
        metricsRow.revalidate();
        metricsRow.repaint();

        // ----- charts -----
        JFreeChart barChart = ChartFactory.createBarChart(
                "Top 5 Produk Terlaris", "Produk", "Jumlah Terjual",
                barData, PlotOrientation.VERTICAL, false, true, false);
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Distribusi Status Pesanan", pieData, true, true, false);
        styleChart(barChart);
        styleChart(pieChart);

        chartsRow.removeAll();
        chartsRow.add(chartCard(barChart));
        chartsRow.add(chartCard(pieChart));
        chartsRow.revalidate();
        chartsRow.repaint();
    }

    private JPanel metricCard(String label, String value, Color accent) {
        JPanel card = new JPanel();
        card.setBackground(UITheme.SURFACE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new javax.swing.border.CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));

        JLabel accentBar = new JLabel();
        accentBar.setOpaque(true);
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(0, 4));
        accentBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        accentBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComp = UITheme.muted(label);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueComp.setForeground(UITheme.TEXT);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(accentBar);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(labelComp);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(valueComp);
        return card;
    }

    private JPanel chartCard(JFreeChart chart) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.SURFACE);
        card.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(UITheme.SURFACE);
        chartPanel.setPopupMenu(null);
        card.add(chartPanel, BorderLayout.CENTER);
        return card;
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(UITheme.SURFACE);
        chart.getTitle().setPaint(UITheme.TEXT);
    }

    private void exportMonthlyReport() {
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String[] monthNames = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"};

        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(currentYear, 2000, currentYear + 5, 1));
        JComboBox<String> monthCombo = new JComboBox<>(monthNames);
        monthCombo.setSelectedIndex(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH));

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.add(new JLabel("Bulan:"));
        form.add(monthCombo);
        form.add(new JLabel("Tahun:"));
        form.add(yearSpinner);

        int result = JOptionPane.showConfirmDialog(this, form, "Pilih Periode Laporan",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        int year = (int) yearSpinner.getValue();
        int month = monthCombo.getSelectedIndex() + 1;

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(String.format("laporan-penjualan-%04d-%02d.pdf", year, month)));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dest = OrderManagementPanel.ensurePdfExtension(chooser.getSelectedFile());

        try {
            ReportExporter.exportMonthlySalesReport(year, month, dest);
            JOptionPane.showMessageDialog(this, "Laporan tersimpan: " + dest.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal membuat laporan: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}