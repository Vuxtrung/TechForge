-- =========================================================
-- TechForge Database
-- =========================================================

DROP DATABASE IF EXISTS techforge;
CREATE DATABASE techforge
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE techforge;

-- =========================================================
-- 1. USERS
-- =========================================================

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    full_name VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(500),

    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_role
        CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE = InnoDB;


-- =========================================================
-- 2. CATEGORIES
-- =========================================================

CREATE TABLE categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;


-- =========================================================
-- 3. BRANDS
-- =========================================================

CREATE TABLE brands (
    brand_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL UNIQUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;


-- =========================================================
-- 4. PRODUCTS
-- =========================================================

CREATE TABLE products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    category_id BIGINT NOT NULL,
    brand_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    price DECIMAL(15, 2) NOT NULL DEFAULT 0,
    stock_quantity INT NOT NULL DEFAULT 0,

    image_url VARCHAR(1000),

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id)
        REFERENCES categories(category_id),

    CONSTRAINT fk_products_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands(brand_id),

    CONSTRAINT chk_products_price
        CHECK (price >= 0),

    CONSTRAINT chk_products_stock
        CHECK (stock_quantity >= 0),

    CONSTRAINT chk_products_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK')),

    INDEX idx_products_category (category_id),
    INDEX idx_products_brand (brand_id),
    INDEX idx_products_name (name),
    INDEX idx_products_status (status)
) ENGINE = InnoDB;


-- =========================================================
-- 5. CARTS
-- =========================================================

CREATE TABLE carts (
    cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_carts_user
        UNIQUE (user_id)
) ENGINE = InnoDB;


-- =========================================================
-- 6. CART ITEMS
-- =========================================================

CREATE TABLE cart_items (
    cart_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    quantity INT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id)
        REFERENCES carts(cart_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id),

    CONSTRAINT chk_cart_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT uq_cart_product
        UNIQUE (cart_id, product_id),

    INDEX idx_cart_items_product (product_id)
) ENGINE = InnoDB;


-- =========================================================
-- 7. ORDERS
-- =========================================================

CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    shipping_address VARCHAR(500) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT chk_orders_total
        CHECK (total_amount >= 0),

    CONSTRAINT chk_orders_status
        CHECK (
            status IN (
                'PENDING',
                'CONFIRMED',
                'SHIPPING',
                'DELIVERED',
                'CANCELLED'
            )
        ),

    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created_at (created_at)
) ENGINE = InnoDB;


-- =========================================================
-- 8. ORDER ITEMS
-- =========================================================

CREATE TABLE order_items (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    quantity INT NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id),

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_price
        CHECK (unit_price >= 0),

    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id)
) ENGINE = InnoDB;


-- =========================================================
-- DONE
-- =========================================================

SELECT 'TechForge database created successfully!' AS message;

SHOW TABLES;