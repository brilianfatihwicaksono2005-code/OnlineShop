package dao;

import config.DBConnection;
import model.OrderStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAO - siklus hidup pesanan + analitik penjualan.
 *
 * Proses {@link #checkout(int)} dijalankan dalam SATU transaksi ACID:
 *   setAutoCommit(false) -> insert order/items/sales -> update stok -> clear cart -> commit.
 * Jika ada error (stok kurang, koneksi putus, dsb) seluruh perubahan di-rollback.
 */
public final class OrderDAO {

    private OrderDAO() { }

    private static final class Line {
        int productId;
        String name;
        double unitPrice;
        int quantity;
        int stock;
        double subtotal;
    }

    // =========================================================
    // CHECKOUT (TRANSAKSI ACID)
    // =========================================================
    /**
     * Memproses checkout keranjang milik user secara atomik.
     * @return order_id yang baru dibuat.
     */
    public static int checkout(int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            List<Line> lines = new ArrayList<>();
            String selectSql = "SELECT c.product_id, p.name, p.price, c.quantity, p.stock " +
                    "FROM shopping_cart c JOIN products p ON c.product_id = p.product_id " +
                    "WHERE c.user_id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Line l = new Line();
                        l.productId = rs.getInt("product_id");
                        l.name = rs.getString("name");
                        l.unitPrice = rs.getDouble("price");
                        l.quantity = rs.getInt("quantity");
                        l.stock = rs.getInt("stock");
                        l.subtotal = l.unitPrice * l.quantity;
                        if (l.quantity > l.stock) {
                            throw new SQLException("Stok untuk \"" + l.name + "\" tidak cukup. " +
                                    "Tersedia: " + l.stock + ", dipesan: " + l.quantity);
                        }
                        lines.add(l);
                    }
                }
            }

            if (lines.isEmpty()) {
                throw new SQLException("Keranjang kosong.");
            }

            double total = 0;
            for (Line l : lines) total += l.subtotal;

            // 1) buat order (status awal: pending)
            int orderId;
            String insertOrder = "INSERT INTO orders (user_id, total_amount, order_status, payment_status) " +
                    "VALUES (?, ?, 'pending', 'paid')";
            try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setDouble(2, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Gagal mendapatkan order_id.");
                    orderId = keys.getInt(1);
                }
            }

            String insertItem = "INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?)";
            String insertSales = "INSERT INTO sales_history " +
                    "(product_id, product_name, quantity, unit_price, subtotal, order_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            String updateStock = "UPDATE products SET stock = stock - ? WHERE product_id = ?";

            for (Line l : lines) {
                try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, l.productId);
                    ps.setInt(3, l.quantity);
                    ps.setDouble(4, l.unitPrice);
                    ps.setDouble(5, l.subtotal);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(insertSales)) {
                    ps.setInt(1, l.productId);
                    ps.setString(2, l.name);
                    ps.setInt(3, l.quantity);
                    ps.setDouble(4, l.unitPrice);
                    ps.setDouble(5, l.subtotal);
                    ps.setInt(6, orderId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(updateStock)) {
                    ps.setInt(1, l.quantity);
                    ps.setInt(2, l.productId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM shopping_cart WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            conn.commit();
            return orderId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rb) {
                    rb.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) { }
                try {
                    conn.close();
                } catch (SQLException ignored) { }
            }
        }
    }

    // =========================================================
    // QUERY PESANAN
    // =========================================================
    /** {order_id, order_date, total_amount, order_status} untuk satu user. */
    public static List<Object[]> getOrdersByUser(int userId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT order_id, order_date, total_amount, order_status " +
                "FROM orders WHERE user_id = ? ORDER BY order_date DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("order_id"),
                            rs.getTimestamp("order_date"),
                            rs.getDouble("total_amount"),
                            rs.getString("order_status")
                    });
                }
            }
        }
        return rows;
    }

    /** {order_id, username, order_date, total_amount, order_status} untuk admin. */
    public static List<Object[]> getAllOrders(String statusDbFilter) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT o.order_id, u.username, o.order_date, o.total_amount, o.order_status " +
                        "FROM orders o JOIN users u ON o.user_id = u.user_id ");
        if (statusDbFilter != null) sql.append("WHERE o.order_status = ? ");
        sql.append("ORDER BY o.order_date DESC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (statusDbFilter != null) ps.setString(1, statusDbFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("order_id"),
                            rs.getString("username"),
                            rs.getTimestamp("order_date"),
                            rs.getDouble("total_amount"),
                            rs.getString("order_status")
                    });
                }
            }
        }
        return rows;
    }

    public static void updateStatus(int orderId, OrderStatus status) throws SQLException {
        String sql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.getDbValue());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    /** Menghapus seluruh riwayat pesanan user. order_items & sales_history ikut via ON DELETE CASCADE. */
    public static void deleteOrderHistory(int userId) throws SQLException {
        String sql = "DELETE FROM orders WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** {name, quantity, unit_price, subtotal} untuk detail/invoice. */
    public static List<Object[]> getOrderItems(int orderId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT p.name, oi.quantity, oi.unit_price, oi.subtotal " +
                "FROM order_items oi JOIN products p ON oi.product_id = p.product_id " +
                "WHERE oi.order_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("subtotal")
                    });
                }
            }
        }
        return rows;
    }

    /**
     * Header pesanan untuk invoice:
     * {user_id, order_date(Timestamp), total_amount, order_status, payment_status, username}
     */
    public static Object[] getOrderHeader(int orderId) throws SQLException {
        String sql = "SELECT o.user_id, o.order_date, o.total_amount, o.order_status, o.payment_status, u.username " +
                "FROM orders o JOIN users u ON o.user_id = u.user_id WHERE o.order_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                            rs.getInt("user_id"),
                            rs.getTimestamp("order_date"),
                            rs.getDouble("total_amount"),
                            rs.getString("order_status"),
                            rs.getString("payment_status"),
                            rs.getString("username")
                    };
                }
            }
        }
        return null;
    }

    // =========================================================
    // ANALITIK
    // =========================================================
    public static double getTotalRevenue() throws SQLException {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(subtotal),0) FROM sales_history");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public static int countActiveOrders() throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE order_status NOT IN ('selesai','cancelled')";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static int countAllOrders() throws SQLException {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /** {product_name, qty} terlaris. */
    public static List<Object[]> getTopProducts(int limit) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT product_name, SUM(quantity) qty FROM sales_history " +
                "GROUP BY product_name ORDER BY qty DESC LIMIT ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{ rs.getString("product_name"), rs.getInt("qty") });
                }
            }
        }
        return rows;
    }

    /** {order_status, count} distribusi status. */
    public static List<Object[]> getStatusDistribution() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT order_status, COUNT(*) c FROM orders GROUP BY order_status";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{ rs.getString("order_status"), rs.getInt("c") });
            }
        }
        return rows;
    }

    // =========================================================
    // LAPORAN BULANAN
    // =========================================================
    /** {revenue, orders, items_sold} untuk bulan tertentu. */
    public static double[] getMonthlySummary(int year, int month) throws SQLException {
        String sql = "SELECT COALESCE(SUM(subtotal),0) revenue, COUNT(DISTINCT order_id) orders, " +
                "COALESCE(SUM(quantity),0) items_sold FROM sales_history " +
                "WHERE YEAR(sale_date) = ? AND MONTH(sale_date) = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{ rs.getDouble("revenue"), rs.getDouble("orders"), rs.getDouble("items_sold") };
                }
            }
        }
        return new double[]{0, 0, 0};
    }

    /** {product_name, qty, revenue} per produk pada bulan tertentu. */
    public static List<Object[]> getMonthlyProductSales(int year, int month) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT product_name, SUM(quantity) qty, SUM(subtotal) rev FROM sales_history " +
                "WHERE YEAR(sale_date) = ? AND MONTH(sale_date) = ? " +
                "GROUP BY product_name ORDER BY qty DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{ rs.getString("product_name"), rs.getInt("qty"), rs.getDouble("rev") });
                }
            }
        }
        return rows;
    }
}