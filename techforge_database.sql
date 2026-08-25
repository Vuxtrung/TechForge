DROP DATABASE IF EXISTS techforge_db;
CREATE DATABASE techforge_db;
USE techforge_db;

CREATE TABLE roles(
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(30) UNIQUE NOT NULL
);

CREATE TABLE users(
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(255),
    status ENUM('ACTIVE','LOCKED') DEFAULT 'ACTIVE',
    loyalty_points INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY(role_id) REFERENCES roles(role_id)
);

CREATE TABLE user_addresses(
    address_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(100) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    address_type ENUM('HOME','OFFICE') NOT NULL DEFAULT 'HOME',
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    default_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_default = 1 THEN user_id ELSE NULL END) STORED,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(user_id),
    UNIQUE KEY uk_user_one_default_address(default_user_id)
);

CREATE TABLE categories(
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(100) NOT NULL,
    type ENUM('PC_PRODUCT','PC_COMPONENT') NOT NULL,
    component_type ENUM('CPU','MAINBOARD','RAM','GPU','PSU','CASE_TYPE','COOLER','STORAGE','NONE') DEFAULT 'NONE',
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME(6),
    description VARCHAR(255),
    updated_at DATETIME(6),
    FOREIGN KEY(parent_id) REFERENCES categories(category_id)
);

CREATE TABLE products(
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(100),
    description TEXT,
    base_price DECIMAL(15,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    status ENUM('ACTIVE','HIDDEN') DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY(category_id) REFERENCES categories(category_id)
);

CREATE TABLE cpus(
    product_id BIGINT PRIMARY KEY,
    socket VARCHAR(30) NOT NULL,
    cores INT,
    threads INT,
    base_clock_ghz DECIMAL(4,2),
    boost_clock_ghz DECIMAL(4,2),
    tdp_watt INT,
    has_igpu TINYINT(1) DEFAULT 0,
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE mainboards(
    product_id BIGINT PRIMARY KEY,
    socket VARCHAR(30) NOT NULL,
    chipset VARCHAR(30),
    ram_type ENUM('DDR4','DDR5') NOT NULL,
    ram_slots INT,
    max_ram_gb INT,
    form_factor ENUM('ATX','MICRO_ATX','MINI_ITX','E_ATX') NOT NULL,
    m2_slots INT,
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE rams(
    product_id BIGINT PRIMARY KEY,
    ram_type ENUM('DDR4','DDR5') NOT NULL,
    speed_mhz INT NOT NULL,
    capacity_gb INT NOT NULL,
    modules INT DEFAULT 1,
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE gpus(
    product_id BIGINT PRIMARY KEY,
    vram_gb INT,
    length_mm INT,
    power_connector VARCHAR(50),
    recommended_psu_watt INT,
    slot_width INT DEFAULT 2,
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE psus(
    product_id BIGINT PRIMARY KEY,
    wattage INT NOT NULL,
    efficiency_rating VARCHAR(30),
    modular ENUM('FULL','SEMI','NONE') DEFAULT 'FULL',
    form_factor VARCHAR(20) DEFAULT 'ATX',
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE cases(
    product_id BIGINT PRIMARY KEY,
    form_factor_support VARCHAR(100) NOT NULL,
    max_gpu_length_mm INT,
    max_cooler_height_mm INT,
    max_radiator_mm INT,
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE coolers(
    product_id BIGINT PRIMARY KEY,
    cooler_type ENUM('AIR','AIO') NOT NULL,
    height_mm INT,
    radiator_size_mm INT,
    socket_support VARCHAR(200),
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE storages(
    product_id BIGINT PRIMARY KEY,
    storage_type ENUM('SSD_NVME','SSD_SATA','HDD') NOT NULL,
    interface VARCHAR(30),
    capacity_gb INT NOT NULL,
    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE product_variants(
    variant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_name VARCHAR(100) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    FOREIGN KEY(product_id) REFERENCES products(product_id)
);

CREATE TABLE product_images(
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_primary TINYINT(1) DEFAULT 0,
    FOREIGN KEY(product_id) REFERENCES products(product_id)
);

CREATE TABLE product_specifications(
    spec_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    spec_key VARCHAR(100) NOT NULL,
    spec_value VARCHAR(255) NOT NULL,
    FOREIGN KEY(product_id) REFERENCES products(product_id)
);

CREATE TABLE vouchers(
    voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    discount_type ENUM('PERCENT','FIXED_AMOUNT','COMBO_GIFT') NOT NULL,
    discount_value DECIMAL(15,2) NOT NULL,
    min_order_value DECIMAL(15,2) DEFAULT 0.00,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    usage_limit INT,
    max_usage_per_customer INT DEFAULT 1,
    is_active TINYINT(1) DEFAULT 1
);

CREATE TABLE orders(
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_id BIGINT DEFAULT NULL,
    pickup_showroom VARCHAR(100),
    order_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING','CONFIRMED','SHIPPING','DELIVERED','CANCEL_REQUESTED','CANCELLED') DEFAULT 'PENDING',
    total_amount DECIMAL(15,2) NOT NULL,
    voucher_id BIGINT,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    cancel_reason VARCHAR(255),
    email VARCHAR(255),
    order_note VARCHAR(255),
    phone VARCHAR(255),
    recipient_name VARCHAR(255),
    shipping_address VARCHAR(255),
    shipping_fee DECIMAL(15,2),
    shipping_method VARCHAR(255),
    FOREIGN KEY(user_id) REFERENCES users(user_id),
    FOREIGN KEY(address_id) REFERENCES user_addresses(address_id),
    FOREIGN KEY(voucher_id) REFERENCES vouchers(voucher_id)
);

CREATE TABLE pc_builds(
    build_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    build_name VARCHAR(150),
    budget_range VARCHAR(50),
    purpose VARCHAR(100),
    total_price DECIMAL(15,2) DEFAULT 0.00,
    status ENUM('DRAFT','SAVED','ADDED_TO_CART') DEFAULT 'DRAFT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(user_id)
);

CREATE TABLE order_items(
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    variant_id BIGINT,
    build_id BIGINT,
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    product_name VARCHAR(255),
    FOREIGN KEY(order_id) REFERENCES orders(order_id),
    FOREIGN KEY(product_id) REFERENCES products(product_id)
);

CREATE TABLE carts(
    cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(user_id)
);

CREATE TABLE cart_items(
    cart_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT,
    variant_id BIGINT,
    build_id BIGINT,
    quantity INT DEFAULT 1,
    FOREIGN KEY(cart_id) REFERENCES carts(cart_id),
    FOREIGN KEY(product_id) REFERENCES products(product_id),
    FOREIGN KEY(variant_id) REFERENCES product_variants(variant_id),
    FOREIGN KEY(build_id) REFERENCES pc_builds(build_id)
);

CREATE TABLE contacts(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6),
    replied_at DATETIME(6) DEFAULT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    subject VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE installment_plans(
    plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    months INT NOT NULL,
    monthly_amount DECIMAL(15,2) NOT NULL,
    status ENUM('ESTIMATED','REGISTERED','APPROVED','REJECTED') DEFAULT 'ESTIMATED',
    FOREIGN KEY(order_id) REFERENCES orders(order_id)
);

CREATE TABLE otp_verification(
    otp_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    email VARCHAR(150) NOT NULL,
    expired_at DATETIME(6) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    purpose ENUM('REGISTER','RESET_PASSWORD','CHECKOUT') NOT NULL,
    verified BIT(1) NOT NULL
);

CREATE TABLE payments(
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    method ENUM('COD','VNPAY') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING','SUCCESS','FAILED') DEFAULT 'PENDING',
    transaction_code VARCHAR(100),
    paid_at DATETIME,
    FOREIGN KEY(order_id) REFERENCES orders(order_id)
);

CREATE TABLE pc_build_items(
    build_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    build_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(15,2) NOT NULL,
    FOREIGN KEY(build_id) REFERENCES pc_builds(build_id),
    FOREIGN KEY(product_id) REFERENCES products(product_id)
);

CREATE TABLE reviews(
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT,
    rating TINYINT NOT NULL,
    comment TEXT,
    status ENUM('PENDING','APPROVED','HIDDEN') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(product_id) REFERENCES products(product_id),
    FOREIGN KEY(user_id) REFERENCES users(user_id),
    CHECK(rating BETWEEN 1 AND 5)
);

CREATE TABLE voucher_usages(
    usage_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    used_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(voucher_id) REFERENCES vouchers(voucher_id),
    FOREIGN KEY(user_id) REFERENCES users(user_id),
    FOREIGN KEY(order_id) REFERENCES orders(order_id)
);

CREATE TABLE warranty_tickets(
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT,
    imei_serial VARCHAR(100) NOT NULL,
    phone_lookup VARCHAR(20),
    issue_desc TINYTEXT,
    status ENUM('SUBMITTED','IN_PROGRESS','REPLACED_1_1','REPAIRED','CLOSED') DEFAULT 'SUBMITTED',
    assigned_staff_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    FOREIGN KEY(user_id) REFERENCES users(user_id),
    FOREIGN KEY(order_item_id) REFERENCES order_items(order_item_id),
    FOREIGN KEY(assigned_staff_id) REFERENCES users(user_id)
);