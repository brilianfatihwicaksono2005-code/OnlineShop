package dao;

import config.DBConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CartDAO - operasi pada shopping_cart.
 * Menambahkan/mengubah kuantitas memakai stored procedure yang sudah memvalidasi stok.
 */
public final class CartDAO {

    private CartDAO() { }

    /** Menambah produk ke keranjang (validasi stok di stored procedure AddToCart). */
    public static void addToCart(int userId, int productId, int quantity) throws SQLException {
        String sql = "{CALL AddToCart(?, ?, ?)}";
        try (Connection conn = DBConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, userId);
            cs.setInt(2, productId);
            cs.setInt(3, quantity);
            cs.execute();
        }
    }

    /**
     * Isi keranjang untuk UI customer.
     * @return baris {cart_id, product_id, name, quantity, price, stock, image}
     */
    public static List<Object[]> getCartItems(int userId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT sc.cart_id, p.product_id, p.name, sc.quantity, p.price, p.stock, p.image " +
                "FROM shopping_cart sc JOIN products p ON sc.product_id = p.product_id " +
                "WHERE sc.user_id = ? ORDER BY p.name";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("cart_id"),
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getDouble("price"),
                            rs.getInt("stock"),
                            rs.getString("image")
                    });
                }
            }
        }
        return rows;
    }

    /** Jumlah produk berbeda dalam keranjang (untuk badge indikator). */
    public static int countDistinctItems(int userId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT product_id) FROM shopping_cart WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /** Mengubah kuantitas item (validasi stok di stored procedure UpdateCartItemQuantity). */
    public static void updateQuantity(int cartId, int quantity) throws SQLException {
        String sql = "{CALL UpdateCartItemQuantity(?, ?)}";
        try (Connection conn = DBConnection.getInstance().getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, cartId);
            cs.setInt(2, quantity);
            cs.execute();
        }
    }

    public static void removeItem(int cartId) throws SQLException {
        String sql = "DELETE FROM shopping_cart WHERE cart_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.executeUpdate();
        }
    }

    public static void removeByProduct(int userId, int productId) throws SQLException {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public static void clearCart(int userId) throws SQLException {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** Baris ringkas untuk tabel keranjang versi lama: {product_id, name, price, quantity, subtotal}. */
    public static List<Object[]> getCartRows(int userId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT p.product_id, p.name, p.price, c.quantity, (p.price * c.quantity) AS subtotal " +
                "FROM shopping_cart c JOIN products p ON c.product_id = p.product_id " +
                "WHERE c.user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity"),
                            rs.getDouble("subtotal")
                    });
                }
            }
        }
        return rows;
    }
}