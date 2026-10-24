package dao;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * ActivityLogDAO - menangani audit trail aktivitas admin pada tabel activity_logs.
 *
 * Metode {@link #log(String, String)} sengaja tidak melempar exception ke pemanggil:
 * kegagalan mencatat log tidak boleh menggagalkan aksi utama aplikasi.
 */
public final class ActivityLogDAO {

    private ActivityLogDAO() { }

    /** Merekam satu aktivitas. Gagal dicatat hanya dilaporkan ke stderr. */
    public static void log(String username, String activity) {
        String sql = "INSERT INTO activity_logs (username, activity) VALUES (?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (username == null || username.isEmpty()) ? "unknown" : username);
            ps.setString(2, activity);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ActivityLogDAO] Gagal menulis log: " + e.getMessage());
        }
    }

    /** Mengambil log terbaru untuk ditampilkan di UI admin. */
    public static List<Object[]> getAll() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT log_id, username, activity, created_at " +
                "FROM activity_logs ORDER BY created_at DESC, log_id DESC LIMIT 500";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp created = rs.getTimestamp("created_at");
                rows.add(new Object[]{
                        rs.getInt("log_id"),
                        rs.getString("username"),
                        rs.getString("activity"),
                        created
                });
            }
        }
        return rows;
    }
}