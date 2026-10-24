package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBConnection - Singleton yang mengelola kolam koneksi (connection pool) HikariCP.
 *
 * Perubahan arsitektur:
 *   - Singleton Pattern : instance hanya dibuat sekali via {@link #getInstance()}.
 *   - Connection Pooling: koneksi fisik dikelola HikariCP, bukan DriverManager tiap request.
 *
 * Pemakaian dari DAO:
 *   try (Connection conn = DBConnection.getInstance().getConnection()) { ... }
 */
public final class DBConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/online_shop_system";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static volatile DBConnection instance;
    private final HikariDataSource dataSource;

    private DBConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("OnlineShopPool");

        // ----- tuning pool -----
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(1_800_000);

        // ----- tuning driver MySQL -----
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
    }

    /** Mengembalikan satu-satunya instance DBConnection (thread-safe, lazy). */
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /** Mengambil koneksi dari pool. Wajib ditutup (cukup close(), otomatis kembali ke pool). */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Menutup seluruh pool. Panggil saat aplikasi berhenti. */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public static void main(String[] args) {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            System.out.println("Koneksi berhasil! (HikariCP pool aktif)");
        } catch (SQLException e) {
            System.out.println("Koneksi gagal: " + e.getMessage());
        } finally {
            DBConnection.getInstance().shutdown();
        }
    }
}