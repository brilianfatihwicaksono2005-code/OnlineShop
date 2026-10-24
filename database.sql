-- =========================================================
-- DATABASE: online_shop_system
-- Script dibuat dari hasil analisis query pada kode Java:
--   Login.java, AdminDashboard.java, CustomerInterface.java,
--   KeranjangInterface.java
-- =========================================================
CREATE DATABASE IF NOT EXISTS online_shop_system
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE online_shop_system;

-- Urutan DROP mengikuti ketergantungan foreign key
DROP TABLE IF EXISTS activity_logs;
DROP TABLE IF EXISTS sales_history;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS shopping_cart;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- ---------------- users ----------------
CREATE TABLE users (
    user_id   INT NOT NULL AUTO_INCREMENT,
    username  VARCHAR(100) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    role      VARCHAR(20)  NOT NULL DEFAULT 'customer',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- products ----------------
CREATE TABLE products (
    product_id  INT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255)  NOT NULL,
    price       DOUBLE        NOT NULL DEFAULT 0,
    stock       INT           NOT NULL DEFAULT 0,
    category    VARCHAR(100)  DEFAULT NULL,
    description TEXT          DEFAULT NULL,
    image       VARCHAR(255)  DEFAULT NULL,
    PRIMARY KEY (product_id),
    KEY idx_products_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- shopping_cart ----------------
CREATE TABLE shopping_cart (
    cart_id    INT NOT NULL AUTO_INCREMENT,
    user_id    INT NOT NULL,
    product_id INT NOT NULL,
    quantity   INT NOT NULL DEFAULT 1,
    PRIMARY KEY (cart_id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    KEY idx_cart_product (product_id),
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)      ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- orders ----------------
CREATE TABLE orders (
    order_id       INT NOT NULL AUTO_INCREMENT,
    user_id        INT NOT NULL,
    order_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount   DOUBLE NOT NULL DEFAULT 0,
    order_status   ENUM('pending','diproses','dikirim','selesai','cancelled') NOT NULL DEFAULT 'pending',
    payment_status ENUM('unpaid','paid') NOT NULL DEFAULT 'unpaid',
    PRIMARY KEY (order_id),
    KEY idx_orders_user (user_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- order_items ----------------
CREATE TABLE order_items (
    order_item_id INT NOT NULL AUTO_INCREMENT,
    order_id      INT NOT NULL,
    product_id    INT NOT NULL,
    quantity      INT NOT NULL DEFAULT 1,
    unit_price    DOUBLE NOT NULL DEFAULT 0,
    subtotal      DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (order_item_id),
    KEY idx_oi_order (order_id),
    KEY idx_oi_product (product_id),
    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id)   REFERENCES orders(order_id)      ON DELETE CASCADE,
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- sales_history ----------------
CREATE TABLE sales_history (
    history_id   INT NOT NULL AUTO_INCREMENT,
    product_id   INT DEFAULT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    unit_price   DOUBLE NOT NULL DEFAULT 0,
    subtotal     DOUBLE NOT NULL DEFAULT 0,
    order_id     INT DEFAULT NULL,
    sale_date    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    KEY idx_sh_order (order_id),
    CONSTRAINT fk_sh_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- activity_logs (AUDIT TRAIL) ----------------
CREATE TABLE activity_logs (
    log_id     INT NOT NULL AUTO_INCREMENT,
    username   VARCHAR(100) NOT NULL,
    activity   TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_logs_username (username),
    KEY idx_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- STORED PROCEDURES yang dipanggil oleh aplikasi Java
-- =========================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS AddToCart$$
CREATE PROCEDURE AddToCart(IN p_user_id INT, IN p_product_id INT, IN p_quantity INT)
BEGIN
    DECLARE v_stock INT;
    DECLARE v_existing INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;
    SELECT stock INTO v_stock FROM products WHERE product_id = p_product_id FOR UPDATE;
    IF v_stock IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Produk tidak ditemukan.';
    END IF;

    SELECT COALESCE(quantity,0) INTO v_existing FROM shopping_cart
        WHERE user_id = p_user_id AND product_id = p_product_id;

    IF (v_existing + p_quantity) > v_stock THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient stock';
    END IF;

    IF v_existing > 0 THEN
        UPDATE shopping_cart SET quantity = quantity + p_quantity
            WHERE user_id = p_user_id AND product_id = p_product_id;
    ELSE
        INSERT INTO shopping_cart (user_id, product_id, quantity)
            VALUES (p_user_id, p_product_id, p_quantity);
    END IF;
    COMMIT;
END$$

DROP PROCEDURE IF EXISTS UpdateCartItemQuantity$$
CREATE PROCEDURE UpdateCartItemQuantity(IN p_cart_id INT, IN p_new_quantity INT)
BEGIN
    DECLARE v_stock INT;
    SELECT p.stock INTO v_stock
        FROM shopping_cart c JOIN products p ON c.product_id = p.product_id
        WHERE c.cart_id = p_cart_id;
    IF p_new_quantity > v_stock THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient stock';
    END IF;
    UPDATE shopping_cart SET quantity = p_new_quantity WHERE cart_id = p_cart_id;
END$$

DROP PROCEDURE IF EXISTS ProcessCheckout$$
CREATE PROCEDURE ProcessCheckout(IN p_user_id INT, OUT p_order_id INT)
BEGIN
    DECLARE v_total DOUBLE DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;
    SELECT COALESCE(SUM(p.price * c.quantity),0) INTO v_total
        FROM shopping_cart c JOIN products p ON c.product_id = p.product_id
        WHERE c.user_id = p_user_id;

    IF v_total = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cart is empty';
    END IF;

    INSERT INTO orders (user_id, total_amount, order_status, payment_status)
        VALUES (p_user_id, v_total, 'pending', 'paid');
    SET p_order_id = LAST_INSERT_ID();

    INSERT INTO sales_history (product_id, product_name, quantity, unit_price, subtotal, order_id)
        SELECT p.product_id, p.name, c.quantity, p.price, (p.price*c.quantity), p_order_id
        FROM shopping_cart c JOIN products p ON c.product_id = p.product_id
        WHERE c.user_id = p_user_id;

    INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
        SELECT p_order_id, p.product_id, c.quantity, p.price, (p.price*c.quantity)
        FROM shopping_cart c JOIN products p ON c.product_id = p.product_id
        WHERE c.user_id = p_user_id;

    UPDATE products p
        JOIN shopping_cart c ON p.product_id = c.product_id
        SET p.stock = p.stock - c.quantity
        WHERE c.user_id = p_user_id;

    DELETE FROM shopping_cart WHERE user_id = p_user_id;
    COMMIT;
END$$

DELIMITER ;

-- =========================================================
-- DATA CONTOH (opsional) - hapus jika tidak diperlukan
-- =========================================================
INSERT INTO users (username, password, role) VALUES
    ('admin', 'admin123', 'admin'),
    ('customer', 'customer123', 'customer');

-- =========================================================
-- MIGRASI STATE MACHINE STATUS PESANAN
-- Jika database sudah ada sebelumnya, jalankan blok di bawah
-- sekali saja untuk mengubah enum order_status:
--
-- ALTER TABLE orders
--   MODIFY order_status ENUM('pending','paid','shipped','delivered','diproses','dikirim','selesai','cancelled')
--   NOT NULL DEFAULT 'pending';
-- UPDATE orders SET order_status='diproses' WHERE order_status='paid';
-- UPDATE orders SET order_status='dikirim'  WHERE order_status='shipped';
-- UPDATE orders SET order_status='selesai'  WHERE order_status='delivered';
-- ALTER TABLE orders
--   MODIFY order_status ENUM('pending','diproses','dikirim','selesai','cancelled')
--   NOT NULL DEFAULT 'pending';
-- =========================================================
