DROP DATABASE IF EXISTS techforge;
CREATE DATABASE techforge
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE techforge;

-- =========================================================
-- 1. ROLES
-- =========================================================

CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(30) NOT NULL UNIQUE
) ENGINE = InnoDB;

INSERT INTO roles (role_id, role_name) VALUES
    (5, 'ADMIN'),
    (2, 'CUSTOMER'),
    (1, 'GUEST'),
    (3, 'STAFF_SALES'),
    (4, 'STAFF_WARRANTY');


-- =========================================================
-- 2. USERS
-- =========================================================

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    role_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(500),
    avatar_url VARCHAR(255),
    status ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    loyalty_points INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT users_ibfk_1
        FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
) ENGINE = InnoDB;

INSERT INTO users (user_id, role_id, full_name, email, password_hash, phone, address, status) VALUES
    (1, 5, 'TechForge Admin', 'admin@techforge.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIui', '0988888888', 'Hà Nội', 'ACTIVE'),
    (2, 2, 'Khách Hàng Mẫu', 'customer@techforge.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIui', '0977777777', 'Hà Nội', 'ACTIVE');


-- =========================================================
-- 3. CATEGORIES
-- =========================================================

CREATE TABLE categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    parent_id BIGINT,
    name VARCHAR(100) NOT NULL UNIQUE,
    type ENUM('PC_PRODUCT','PC_COMPONENT') NOT NULL,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME(6),
    description VARCHAR(255),
    updated_at DATETIME(6),
    CONSTRAINT categories_ibfk_1
        FOREIGN KEY (parent_id)
        REFERENCES categories(category_id)
) ENGINE = InnoDB;


-- =========================================================
-- 4. VOUCHERS
-- =========================================================

CREATE TABLE vouchers (
    voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type ENUM('PERCENT','FIXED_AMOUNT','COMBO_GIFT') NOT NULL,
    discount_value DECIMAL(15,2) NOT NULL,
    min_order_value DECIMAL(15,2) DEFAULT 0.00,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    usage_limit INT,
    is_active TINYINT(1) DEFAULT 1
) ENGINE = InnoDB;


-- =========================================================
-- 5. PRODUCTS
-- =========================================================

CREATE TABLE products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(100),
    description TEXT,
    base_price DECIMAL(15,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE','HIDDEN') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT products_ibfk_1
        FOREIGN KEY (category_id)
        REFERENCES categories(category_id)
) ENGINE = InnoDB;


-- =========================================================
-- 6. PRODUCT VARIANTS
-- =========================================================

CREATE TABLE product_variants (
    variant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_name VARCHAR(100) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,

    CONSTRAINT product_variants_ibfk_1
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
) ENGINE = InnoDB;


-- =========================================================
-- 7. PRODUCT IMAGES
-- =========================================================

CREATE TABLE product_images (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_primary TINYINT(1) DEFAULT 0,

    CONSTRAINT product_images_ibfk_1
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
) ENGINE = InnoDB;


-- =========================================================
-- 8. PRODUCT SPECIFICATIONS
-- =========================================================

CREATE TABLE product_specifications (
    spec_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    spec_key VARCHAR(100) NOT NULL,
    spec_value VARCHAR(255) NOT NULL,

    CONSTRAINT product_specifications_ibfk_1
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
) ENGINE = InnoDB;


-- =========================================================
-- 9. PC BUILDS
-- =========================================================

CREATE TABLE pc_builds (
    build_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    build_name VARCHAR(150),
    budget_range VARCHAR(50),
    purpose VARCHAR(100),
    total_price DECIMAL(15,2) DEFAULT 0.00,
    status ENUM('DRAFT','SAVED','ADDED_TO_CART') DEFAULT 'DRAFT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pc_builds_ibfk_1
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
) ENGINE = InnoDB;


-- =========================================================
-- 10. CARTS
-- =========================================================

CREATE TABLE carts (
    cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,

    UNIQUE KEY uq_carts_user (user_id),

    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 11. ORDERS
-- =========================================================

CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pickup_showroom VARCHAR(100),
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM(
        'PENDING',
        'CONFIRMED',
        'SHIPPING',
        'DELIVERED',
        'CANCEL_REQUESTED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(15,2) NOT NULL,
    voucher_id BIGINT,
    cancel_reason VARCHAR(255),

    CONSTRAINT fk_orders_voucher
        FOREIGN KEY (voucher_id)
        REFERENCES vouchers(voucher_id),

    CONSTRAINT orders_ibfk_1
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
) ENGINE = InnoDB;


-- =========================================================
-- 12. ORDER ITEMS
-- (product_id đổi thành NULLABLE để khớp với bản dump)
-- =========================================================

CREATE TABLE order_items (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    variant_id BIGINT,
    build_id BIGINT,
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,

    CONSTRAINT order_items_ibfk_1
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT order_items_ibfk_2
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
) ENGINE = InnoDB;


-- =========================================================
-- 13. CART ITEMS
-- =========================================================

CREATE TABLE cart_items (
    cart_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT,
    variant_id BIGINT,
    build_id BIGINT,
    quantity INT NOT NULL DEFAULT 1,

    CONSTRAINT cart_items_ibfk_1
        FOREIGN KEY (cart_id)
        REFERENCES carts(cart_id),

    CONSTRAINT cart_items_ibfk_2
        FOREIGN KEY (product_id)
        REFERENCES products(product_id),

    CONSTRAINT cart_items_ibfk_3
        FOREIGN KEY (variant_id)
        REFERENCES product_variants(variant_id),

    CONSTRAINT cart_items_ibfk_4
        FOREIGN KEY (build_id)
        REFERENCES pc_builds(build_id)
) ENGINE = InnoDB;


-- =========================================================
-- 14. PC BUILD ITEMS
-- =========================================================

CREATE TABLE pc_build_items (
    build_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    build_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(15,2) NOT NULL,

    CONSTRAINT pc_build_items_ibfk_1
        FOREIGN KEY (build_id)
        REFERENCES pc_builds(build_id),

    CONSTRAINT pc_build_items_ibfk_2
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
) ENGINE = InnoDB;


-- =========================================================
-- 15. PAYMENTS
-- =========================================================

CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    method ENUM('COD','VNPAY') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
    transaction_code VARCHAR(100),
    paid_at DATETIME,

    CONSTRAINT payments_ibfk_1
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
) ENGINE = InnoDB;


-- =========================================================
-- 16. WARRANTY TICKETS
-- =========================================================

CREATE TABLE warranty_tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT,
    imei_serial VARCHAR(100) NOT NULL,
    phone_lookup VARCHAR(20),
    issue_desc TEXT,
    status ENUM('SUBMITTED','IN_PROGRESS','REPLACED_1_1','REPAIRED','CLOSED')
        DEFAULT 'SUBMITTED',
    assigned_staff_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,

    CONSTRAINT warranty_tickets_ibfk_1
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT warranty_tickets_ibfk_2
        FOREIGN KEY (order_item_id)
        REFERENCES order_items(order_item_id),

    CONSTRAINT warranty_tickets_ibfk_3
        FOREIGN KEY (assigned_staff_id)
        REFERENCES users(user_id)
) ENGINE = InnoDB;


-- =========================================================
-- 17. VOUCHER USAGES
-- =========================================================

CREATE TABLE voucher_usages (
    usage_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    used_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT voucher_usages_ibfk_1
        FOREIGN KEY (voucher_id)
        REFERENCES vouchers(voucher_id),

    CONSTRAINT voucher_usages_ibfk_2
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT voucher_usages_ibfk_3
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
) ENGINE = InnoDB;


-- =========================================================
-- 18. PASSWORD RESET OTP VERIFICATION
-- =========================================================

CREATE TABLE otp_verification (
    otp_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    purpose ENUM('REGISTER','RESET_PASSWORD','CHECKOUT') NOT NULL,
    expired_at DATETIME NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_purpose (email, purpose)
) ENGINE = InnoDB;


-- =========================================================
-- 19. INSTALLMENT PLANS
-- =========================================================

CREATE TABLE installment_plans (
    plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    months INT NOT NULL,
    monthly_amount DECIMAL(15,2) NOT NULL,
    status ENUM('ESTIMATED','REGISTERED','APPROVED','REJECTED') DEFAULT 'ESTIMATED',

    CONSTRAINT installment_plans_ibfk_1
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
) ENGINE = InnoDB;


-- =========================================================
-- 20. REVIEWS
-- =========================================================

CREATE TABLE reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT,
    rating TINYINT NOT NULL,
    comment TEXT,
    status ENUM('PENDING','APPROVED','HIDDEN') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT reviews_ibfk_1
        FOREIGN KEY (product_id)
        REFERENCES products(product_id),

    CONSTRAINT reviews_ibfk_2
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT reviews_chk_1
        CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB;


-- =========================================================
-- 21. COMPATIBILITY RULES (bảng mới, bổ sung để khớp bản dump)
-- =========================================================

CREATE TABLE compatibility_rules (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    component_type_a VARCHAR(50) NOT NULL,
    component_type_b VARCHAR(50) NOT NULL,
    rule_type ENUM('SOCKET_MATCH','PSU_WATTAGE','CASE_SIZE','RAM_TYPE','OTHER') NOT NULL,
    rule_expression VARCHAR(255) NOT NULL,
    is_active TINYINT(1) DEFAULT 1
) ENGINE = InnoDB;


-- =========================================================
-- DONE
-- =========================================================

SELECT 'TechForge database created successfully!' AS message;

SHOW TABLES;