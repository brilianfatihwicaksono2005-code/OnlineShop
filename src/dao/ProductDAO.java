package dao;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductDAO - seluruh operasi CRUD tabel products.
 * GUI tidak lagi menulis SQL; cukup memanggil method di sini.
 */
public final class ProductDAO {

    private ProductDAO() { }

    /** Data produk lengkap untuk tabel admin: {id, name, price, stock, category, description, image}. */
    public static List<Object[]> getAllRows() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT product_id, name, price, stock, category, description, image " +
                "FROM products ORDER BY product_id";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getString("image")
                });
            }
        }
        return rows;
    }

    /**
     * Pencarian real-time dengan LIKE + filter kategori.
     * @param keyword  kata kunci nama produk (boleh kosong)
     * @param category kategori terpilih, atau null untuk semua
     * @return baris {id, name, price, stock, image}
     */
    public static List<Object[]> search(String keyword, String category) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT product_id, name, price, stock, image FROM products WHERE name LIKE ?");
        if (category != null) {
            sql.append(" AND category = ?");
        }
        sql.append(" ORDER BY name");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, "%" + (keyword == null ? "" : keyword) + "%");
            if (category != null) ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("stock"),
                            rs.getString("image")
                    });
                }
            }
        }
        return rows;
    }

    /** Detail satu produk: {name, price, stock, image} atau null. */
    public static Object[] getById(int productId) throws SQLException {
        String sql = "SELECT name, price, stock, image FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("stock"),
                            rs.getString("image")
                    };
                }
            }
        }
        return null;
    }

    /** Daftar kategori unik (tidak null / kosong). */
    public static List<String> getCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products " +
                "WHERE category IS NOT NULL AND TRIM(category) <> '' ORDER BY category";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) categories.add(rs.getString("category"));
        }
        return categories;
    }

    public static void insert(String name, double price, int stock, String category,
                              String description, String image) throws SQLException {
        String sql = "INSERT INTO products (name, price, stock, category, description, image) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, stock);
            ps.setString(4, category);
            ps.setString(5, description);
            ps.setString(6, image);
            ps.executeUpdate();
        }
    }

    public static void update(int productId, String name, double price, int stock, String category,
                              String description, String image) throws SQLException {
        String sql = "UPDATE products SET name=?, price=?, stock=?, category=?, description=?, image=? " +
                "WHERE product_id=?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, stock);
            ps.setString(4, category);
            ps.setString(5, description);
            ps.setString(6, image);
            ps.setInt(7, productId);
            ps.executeUpdate();
        }
    }

    public static void delete(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    /** Jumlah referensi produk pada order_items (untuk mencegah hapus data yang dipakai). */
    public static int countOrderItems(int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM order_items WHERE product_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public static int countAll() throws SQLException {
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}