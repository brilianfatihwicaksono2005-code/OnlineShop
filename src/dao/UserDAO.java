package dao;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * UserDAO - akses data untuk tabel users (autentikasi & info pengguna).
 */
public final class UserDAO {

    private UserDAO() { }

    /** Representasi ringkas pengguna yang berhasil login. */
    public static final class User {
        public final int id;
        public final String username;
        public final String role;

        public User(int id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }

        public boolean isAdmin() {
            return "admin".equalsIgnoreCase(role);
        }
    }

    /**
     * Memvalidasi kredensial.
     * @return User jika cocok, atau null bila username/password salah.
     */
    public static User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("role"));
                }
            }
        }
        return null;
    }

    /** Mengambil username berdasarkan id (untuk keperluan audit/log). */
    public static String getUsernameById(int userId) throws SQLException {
        String sql = "SELECT username FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        }
        return "unknown";
    }
}