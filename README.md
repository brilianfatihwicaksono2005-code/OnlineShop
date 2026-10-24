# Online Shop System

Aplikasi desktop **toko online** berbasis **Java Swing** dengan database **MySQL**. Aplikasi memiliki dua peran pengguna — **Admin** dan **Customer** — dengan antarmuka modern (FlatLaf), katalog produk, keranjang belanja, checkout transaksional, manajemen pesanan, dashboard analitik, dan ekspor laporan PDF.

---

## Daftar Isi

- [Fitur](#fitur)
- [Teknologi](#teknologi)
- [Struktur Proyek](#struktur-proyek)
- [Prasyarat](#prasyarat)
- [Instalasi](#instalasi)
- [Menjalankan Aplikasi](#menjalankan-aplikasi)
- [Akun Demo](#akun-demo)
- [Struktur Database](#struktur-database)
- [Catatan Arsitektur](#catatan-arsitektur)
- [Troubleshooting](#troubleshooting)

---

## Fitur

### Autentikasi & Sesi

- Login dengan username/password untuk peran **admin** dan **customer**.
- Sesi pengguna aktif disimpan melalui `model.Session` dan diteruskan antar halaman.

### Admin

- **Kelola produk**: tambah, ubah, hapus produk, serta unggah gambar produk.
- **Kelola pesanan**: ubah status mengikuti *state machine* `Pending → Diproses → Dikirim → Selesai`; pembatalan hanya dapat dilakukan saat status masih `Pending`.
- **Dashboard analitik**: total pendapatan, pesanan aktif, jumlah pesanan, produk terlaris, grafik batang 5 produk terlaris, dan diagram pie distribusi status pesanan (JFreeChart).
- **Ekspor PDF**: invoice per pesanan dan laporan penjualan bulanan.
- **Log aktivitas**: audit trail aktivitas admin (tambah/ubah/hapus produk, ubah status pesanan, login).

### Customer

- **Katalog produk** dengan pencarian *real-time* dan filter kategori.
- **Detail produk** dalam dialog, termasuk validasi stok.
- **Keranjang belanja**: tambah item, ubah kuantitas, hapus item, dan kosongkan keranjang.
- **Checkout** dengan validasi stok di sisi database dan proses transaksi **ACID** (rollback otomatis bila gagal).
- **Riwayat pesanan**: daftar pesanan, detail item, hapus riwayat, dan unduh struk/invoice PDF.

### Teknis

- **Connection pooling** dengan HikariCP melalui `DBConnection` (Singleton).
- **Pola DAO** — seluruh query SQL terpusat di package `dao`.
- **Prepared Statement** dan **stored procedure** untuk keamanan & validasi stok.
- Tema UI terpusat di `util.UITheme` (FlatLaf, Light Mode) sehingga tampilan konsisten.
- Tema, komponen, dan helper ekspor laporan dipisahkan ke package `util`.

---

## Teknologi

| Komponen | Versi / Keterangan |
| --- | --- |
| Java | JDK 21+ (diuji pada JDK 21) |
| GUI | Java Swing |
| Look & Feel | [FlatLaf](https://www.formdev.com/flatlaf/) 3.2.1 |
| Database | MySQL 8+ (Connector/J) |
| Connection Pool | HikariCP 5.1.0 |
| Grafik | JFreeChart 1.5.4 |
| Ekspor PDF | iText 5.5.13.3 |
| Logging | SLF4J 2.0.13 |
| Build | IntelliJ IDEA / VS Code / javac manual |

Seluruh library sudah tersedia di folder [`lib/`](lib), sehingga tidak perlu mengunduh ulang.

---

## Struktur Proyek

```text
OnlineShop/
├── src/
│   ├── OnlineShopSystem.java        # Entry point (main class)
│   ├── config/
│   │   └── DBConnection.java        # Singleton + HikariCP connection pool
│   ├── model/
│   │   ├── Product.java
│   │   ├── CartItem.java
│   │   ├── OrderStatus.java         # State machine status pesanan
│   │   └── Session.java             # Sesi user yang sedang login
│   ├── dao/
│   │   ├── ActivityLogDAO.java
│   │   ├── CartDAO.java
│   │   ├── OrderDAO.java
│   │   ├── ProductDAO.java
│   │   └── UserDAO.java
│   ├── util/
│   │   ├── UITheme.java             # Palet warna, font, komponen UI
│   │   └── ReportExporter.java      # Ekspor invoice & laporan PDF
│   └── view/
│       ├── Login.java
│       ├── AdminDashboard.java
│       ├── CustomerInterface.java
│       ├── KeranjangInterface.java
│       ├── ActivityLogPanel.java
│       ├── AnalyticsPanel.java
│       └── OrderManagementPanel.java
├── lib/                             # Dependency JAR
├── images/                          # Gambar produk (path relatif aplikasi)
├── database.sql                     # Skema database + stored procedure + seed
├── OnlineShop-master.iml            # Konfigurasi modul IntelliJ
├── .vscode/settings.json            # Konfigurasi Java project untuk VS Code
└── README.md
```

---

## Prasyarat

1. **JDK 21** atau lebih baru (`java -version`).
2. **MySQL Server 8+** yang berjalan di `localhost:3306`.
3. Salah satu IDE (opsional): IntelliJ IDEA atau VS Code + *Extension Pack for Java*.

---

## Instalasi

### 1. Clone repository

```bash
git clone https://github.com/<username>/<nama-repo>.git
cd <nama-repo>
```

### 2. Buat database

Impor file [`database.sql`](database.sql) — file ini membuat database `online_shop_system`, seluruh tabel, stored procedure, dan akun demo.

**MySQL Workbench / phpMyAdmin:** buka file `database.sql`, lalu jalankan seluruh script.

**Terminal (cmd.exe):**

```cmd
mysql -u root -p < database.sql
```

**Terminal PowerShell** tidak mendukung operator `<`, gunakan:

```powershell
cmd /c "mysql -u root -p < database.sql"
```

> Pastikan script dijalankan sampai selesai, termasuk blok `DELIMITER` untuk stored procedure `AddToCart`, `UpdateCartItemQuantity`, dan `ProcessCheckout`.

### 3. Konfigurasi koneksi database

Buka [`src/config/DBConnection.java`](src/config/DBConnection.java) dan sesuaikan kredensial MySQL:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/online_shop_system";
private static final String USER     = "root";
private static final String PASSWORD = "";
```

Jika server MySQL Anda memakai autentikasi `caching_sha2_password` (default MySQL 8+) dan koneksi gagal, ubah URL menjadi:

```java
private static final String URL = "jdbc:mysql://localhost:3306/online_shop_system?allowPublicKeyRetrieval=true&useSSL=false";
```

---

## Menjalankan Aplikasi

### IntelliJ IDEA

1. **File → Open** dan pilih folder proyek (yang berisi `OnlineShop-master.iml`).
2. Tunggu proses indexing selesai. Folder `src` sudah ditandai sebagai *Sources Root* dan seluruh JAR di `lib/` sudah terdaftar sebagai dependency.
3. Buka `src/OnlineShopSystem.java`, klik kanan → **Run 'OnlineShopSystem.main()'**.

### VS Code

1. Install **Extension Pack for Java**.
2. Buka folder proyek. Konfigurasi `src` dan `lib/**/*.jar` sudah tersedia di `.vscode/settings.json`.
3. Buka `src/OnlineShopSystem.java` → klik **Run**.

### Command Line (tanpa IDE)

Dari root proyek (PowerShell):

```powershell
# Kompilasi (semua dependency dari lib/)
javac -encoding UTF-8 -d out -cp "lib/*" -sourcepath src src/OnlineShopSystem.java

# Jalankan
java -cp "out;lib/*" OnlineShopSystem
```

> Di Linux/macOS gunakan pemisah classpath `:` — misalnya `java -cp "out:lib/*" OnlineShopSystem`.

> **Catatan:** jalankan aplikasi dari **root proyek** agar folder `images/` (path relatif untuk gambar produk) dapat ditemukan.

Beberapa kelas juga memiliki `main` sendiri untuk pengujian halaman secara terpisah (`Login`, `CustomerInterface`, `KeranjangInterface`, `AdminDashboard`).

---

## Akun Demo

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `admin123` | Admin |
| `customer` | `customer123` | Customer |

> Password pada proyek ini disimpan dalam bentuk *plain text* di database untuk keperluan pembelajaran. Untuk penggunaan produksi, gunakan hashing (mis. BCrypt).

---

## Struktur Database

Database: `online_shop_system`

| Tabel | Fungsi |
| --- | --- |
| `users` | Data pengguna dan peran (`admin`/`customer`) |
| `products` | Katalog produk (harga, stok, kategori, deskripsi, gambar) |
| `shopping_cart` | Item keranjang per pengguna |
| `orders` | Header pesanan (total, status, status pembayaran) |
| `order_items` | Rincian item per pesanan |
| `sales_history` | Riwayat penjualan untuk analitik & laporan |
| `activity_logs` | Audit trail aktivitas admin |

Stored procedure: `AddToCart`, `UpdateCartItemQuantity`, `ProcessCheckout`.

---

## Catatan Arsitektur

- **`DBConnection`** menggunakan *double-checked locking* untuk membuat satu instance `HikariDataSource`. Koneksi diperoleh via `DBConnection.getInstance().getConnection()` dan otomatis kembali ke pool saat `close()`.
- **`OrderDAO.checkout()`** menjalankan seluruh proses (insert order, order items, sales history, update stok, kosongkan keranjang) dalam satu transaksi dengan `setAutoCommit(false)` dan `rollback()` saat terjadi error.
- **`OrderStatus`** adalah *enum* yang memvalidasi transisi status pesanan, sehingga perubahan status tidak bisa melompati alur.
- **`UITheme`** memusatkan palet warna, font, serta pabrik komponen (tombol, tabel, card), sehingga halaman baru otomatis konsisten.
- **`ReportExporter`** membuat PDF invoice dan laporan penjualan bulanan menggunakan iText.

---

## Troubleshooting

| Masalah | Solusi |
| --- | --- |
| `Unknown database 'online_shop_system'` | Impor `database.sql` terlebih dahulu. |
| `Access denied for user 'root'@'localhost'` | Sesuaikan `USER`/`PASSWORD` di `DBConnection.java`. |
| `Public Key Retrieval is not allowed` | Tambahkan `?allowPublicKeyRetrieval=true&useSSL=false` pada URL JDBC. |
| `Communications link failure` | Pastikan MySQL Server berjalan di port `3306`. |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | Pastikan seluruh JAR di `lib/` masuk classpath (`-cp "lib/*"` atau dependency IDE). |
| Gambar produk tidak tampil | Jalankan aplikasi dari root proyek agar folder `images/` ditemukan. |
| Error karakter/encoding saat kompilasi | Gunakan `javac -encoding UTF-8`. |
| Prosedur `AddToCart` tidak ditemukan | Jalankan ulang `database.sql` sampai blok `DELIMITER` selesai. |

---

## Lisensi

Proyek ini dibuat untuk keperluan pembelajaran mata kuliah dan tidak memiliki lisensi khusus. Silakan gunakan dan modifikasi sesuai kebutuhan.
