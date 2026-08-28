-- ============================================================
-- TechForge - Clean Schema Script (no dummy/test data)
-- Target: techforge_database
-- Generated from techforge_db dump, structure only + lookup seed (roles)
-- ============================================================

DROP DATABASE IF EXISTS `techforge_database`;
CREATE DATABASE `techforge_database` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `techforge_database`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- roles (lookup table - seeded, needed as FK target)
-- ------------------------------------------------------------
CREATE TABLE `roles` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(30) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `roles` (`role_id`, `role_name`) VALUES
  (1,'GUEST'),
  (2,'CUSTOMER'),
  (3,'STAFF_SALES'),
  (4,'STAFF_WARRANTY'),
  (5,'ADMIN');

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `status` enum('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  `loyalty_points` int NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `address` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- user_addresses
-- ------------------------------------------------------------
CREATE TABLE `user_addresses` (
  `address_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `recipient_name` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `address_line` varchar(255) NOT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `province` varchar(100) NOT NULL,
  `ward` varchar(100) NOT NULL,
  `type` enum('HOME','OFFICE') NOT NULL,
  `default_user_id` bigint GENERATED ALWAYS AS ((case when (`is_default` = 1) then `user_id` else NULL end)) STORED,
  PRIMARY KEY (`address_id`),
  UNIQUE KEY `uk_user_one_default_address` (`default_user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_addresses_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- categories
-- ------------------------------------------------------------
CREATE TABLE `categories` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `type` enum('PC_PRODUCT','PC_COMPONENT') NOT NULL,
  `component_type` enum('CPU','MAINBOARD','RAM','GPU','PSU','CASE_TYPE','COOLER','STORAGE','NONE') DEFAULT 'NONE',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`category_id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `categories_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- products
-- ------------------------------------------------------------
CREATE TABLE `products` (
  `product_id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `name` varchar(200) NOT NULL,
  `brand` varchar(100) DEFAULT NULL,
  `description` text,
  `base_price` decimal(15,2) NOT NULL,
  `stock_quantity` int NOT NULL DEFAULT '0',
  `status` enum('ACTIVE','HIDDEN') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  KEY `category_id` (`category_id`),
  CONSTRAINT `products_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- product_images
-- ------------------------------------------------------------
CREATE TABLE `product_images` (
  `image_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `image_url` varchar(255) NOT NULL,
  `is_primary` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`image_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `product_images_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- product_specifications (EAV - PC_PRODUCT only)
-- ------------------------------------------------------------
CREATE TABLE `product_specifications` (
  `spec_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `spec_key` varchar(100) NOT NULL,
  `spec_value` varchar(255) NOT NULL,
  PRIMARY KEY (`spec_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `product_specifications_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- product_variants
-- ------------------------------------------------------------
CREATE TABLE `product_variants` (
  `variant_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `variant_name` varchar(100) NOT NULL,
  `price` decimal(15,2) NOT NULL,
  `stock_quantity` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`variant_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `product_variants_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Table-per-component: cpus, mainboards, rams, gpus, psus, coolers, cases, storages
-- ------------------------------------------------------------
CREATE TABLE `cpus` (
  `product_id` bigint NOT NULL,
  `socket` varchar(30) NOT NULL,
  `cores` int DEFAULT NULL,
  `threads` int DEFAULT NULL,
  `base_clock_ghz` decimal(4,2) DEFAULT NULL,
  `boost_clock_ghz` decimal(4,2) DEFAULT NULL,
  `tdp_watt` int DEFAULT NULL,
  `has_igpu` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`product_id`),
  KEY `idx_cpus_socket` (`socket`),
  CONSTRAINT `cpus_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mainboards` (
  `product_id` bigint NOT NULL,
  `socket` varchar(30) NOT NULL,
  `chipset` varchar(30) DEFAULT NULL,
  `ram_type` enum('DDR4','DDR5') NOT NULL,
  `ram_slots` int DEFAULT NULL,
  `max_ram_gb` int DEFAULT NULL,
  `form_factor` enum('ATX','MICRO_ATX','MINI_ITX','E_ATX') NOT NULL,
  `m2_slots` int DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  KEY `idx_mb_socket` (`socket`),
  KEY `idx_mb_ram_type` (`ram_type`),
  KEY `idx_mb_form_factor` (`form_factor`),
  CONSTRAINT `mainboards_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `rams` (
  `product_id` bigint NOT NULL,
  `ram_type` enum('DDR4','DDR5') NOT NULL,
  `speed_mhz` int NOT NULL,
  `capacity_gb` int NOT NULL,
  `modules` int DEFAULT '1',
  PRIMARY KEY (`product_id`),
  KEY `idx_ram_type` (`ram_type`),
  CONSTRAINT `rams_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `gpus` (
  `product_id` bigint NOT NULL,
  `vram_gb` int DEFAULT NULL,
  `length_mm` int DEFAULT NULL,
  `power_connector` varchar(50) DEFAULT NULL,
  `recommended_psu_watt` int DEFAULT NULL,
  `slot_width` int DEFAULT '2',
  PRIMARY KEY (`product_id`),
  CONSTRAINT `gpus_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `psus` (
  `product_id` bigint NOT NULL,
  `wattage` int NOT NULL,
  `efficiency_rating` varchar(30) DEFAULT NULL,
  `modular` enum('FULL','SEMI','NONE') DEFAULT 'FULL',
  `form_factor` varchar(20) DEFAULT 'ATX',
  PRIMARY KEY (`product_id`),
  CONSTRAINT `psus_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coolers` (
  `product_id` bigint NOT NULL,
  `cooler_type` enum('AIR','AIO') NOT NULL,
  `height_mm` int DEFAULT NULL,
  `radiator_size_mm` int DEFAULT NULL,
  `socket_support` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  CONSTRAINT `coolers_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cases` (
  `product_id` bigint NOT NULL,
  `form_factor_support` varchar(100) NOT NULL,
  `max_gpu_length_mm` int DEFAULT NULL,
  `max_cooler_height_mm` int DEFAULT NULL,
  `max_radiator_mm` int DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  CONSTRAINT `cases_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `storages` (
  `product_id` bigint NOT NULL,
  `storage_type` enum('SSD_NVME','SSD_SATA','HDD') NOT NULL,
  `interface` varchar(30) DEFAULT NULL,
  `capacity_gb` int NOT NULL,
  PRIMARY KEY (`product_id`),
  CONSTRAINT `storages_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- pc_builds / pc_build_items
-- ------------------------------------------------------------
CREATE TABLE `pc_builds` (
  `build_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `build_name` varchar(150) DEFAULT NULL,
  `budget_range` varchar(50) DEFAULT NULL,
  `purpose` varchar(100) DEFAULT NULL,
  `total_price` decimal(15,2) DEFAULT '0.00',
  `status` enum('DRAFT','SAVED','ADDED_TO_CART') DEFAULT 'DRAFT',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`build_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `pc_builds_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pc_build_items` (
  `build_item_id` bigint NOT NULL AUTO_INCREMENT,
  `build_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `unit_price` decimal(15,2) NOT NULL,
  PRIMARY KEY (`build_item_id`),
  KEY `build_id` (`build_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `pc_build_items_ibfk_1` FOREIGN KEY (`build_id`) REFERENCES `pc_builds` (`build_id`),
  CONSTRAINT `pc_build_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- carts / cart_items (legacy) & user_cart_items (active)
-- ------------------------------------------------------------
CREATE TABLE `carts` (
  `cart_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`cart_id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `carts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cart_items` (
  `cart_item_id` bigint NOT NULL AUTO_INCREMENT,
  `cart_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `variant_id` bigint DEFAULT NULL,
  `build_id` bigint DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`cart_item_id`),
  KEY `cart_id` (`cart_id`),
  KEY `product_id` (`product_id`),
  KEY `variant_id` (`variant_id`),
  KEY `build_id` (`build_id`),
  CONSTRAINT `cart_items_ibfk_1` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`cart_id`),
  CONSTRAINT `cart_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `cart_items_ibfk_3` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`variant_id`),
  CONSTRAINT `cart_items_ibfk_4` FOREIGN KEY (`build_id`) REFERENCES `pc_builds` (`build_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_cart_items` (
  `cart_item_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `quantity` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`cart_item_id`),
  UNIQUE KEY `uk_user_product_cart` (`user_id`,`product_id`),
  KEY `FKs1hkeemaje03yrqqldcyb559a` (`product_id`),
  CONSTRAINT `FKb6lavbr80jjnkl0xrv7vn1gfi` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKs1hkeemaje03yrqqldcyb559a` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- vouchers / voucher_usages
-- ------------------------------------------------------------
CREATE TABLE `vouchers` (
  `voucher_id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `discount_type` enum('PERCENT','FIXED_AMOUNT','COMBO_GIFT') NOT NULL,
  `discount_value` decimal(15,2) NOT NULL,
  `min_order_value` decimal(15,2) DEFAULT '0.00',
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `usage_limit` int DEFAULT NULL,
  `max_usage_per_customer` int DEFAULT '1',
  `is_active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`voucher_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- orders / order_items / payments
-- ------------------------------------------------------------
CREATE TABLE `orders` (
  `order_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `address_id` bigint DEFAULT NULL,
  `pickup_showroom` varchar(100) DEFAULT NULL,
  `order_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` enum('PENDING','CONFIRMED','SHIPPING','DELIVERED','COMPLETED','COMPLAINT','CANCEL_REQUESTED','CANCELLED') DEFAULT 'PENDING',
  `total_amount` decimal(15,2) NOT NULL,
  `voucher_id` bigint DEFAULT NULL,
  `discount_amount` decimal(15,2) DEFAULT '0.00',
  `cancel_reason` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `order_note` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `recipient_name` varchar(255) DEFAULT NULL,
  `shipping_address` varchar(255) DEFAULT NULL,
  `shipping_fee` decimal(15,2) DEFAULT NULL,
  `shipping_method` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `user_id` (`user_id`),
  KEY `address_id` (`address_id`),
  KEY `fk_orders_voucher` (`voucher_id`),
  CONSTRAINT `fk_orders_voucher` FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`voucher_id`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `orders_ibfk_2` FOREIGN KEY (`address_id`) REFERENCES `user_addresses` (`address_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_items` (
  `order_item_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `variant_id` bigint DEFAULT NULL,
  `build_id` bigint DEFAULT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(15,2) NOT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `is_reviewed` bit(1) NOT NULL,
  PRIMARY KEY (`order_item_id`),
  KEY `order_id` (`order_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `order_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payments` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `method` enum('COD','VNPAY') NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `status` enum('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
  `transaction_code` varchar(100) DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `order_id` (`order_id`),
  CONSTRAINT `payments_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `voucher_usages` (
  `usage_id` bigint NOT NULL AUTO_INCREMENT,
  `voucher_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `used_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`usage_id`),
  KEY `voucher_id` (`voucher_id`),
  KEY `user_id` (`user_id`),
  KEY `order_id` (`order_id`),
  CONSTRAINT `voucher_usages_ibfk_1` FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`voucher_id`),
  CONSTRAINT `voucher_usages_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `voucher_usages_ibfk_3` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- reviews
-- ------------------------------------------------------------
CREATE TABLE `reviews` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `order_item_id` bigint DEFAULT NULL,
  `rating` int NOT NULL,
  `comment` text,
  `status` enum('PENDING','APPROVED','HIDDEN') DEFAULT 'PENDING',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `product_id` (`product_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `reviews_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `reviews_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `reviews_chk_1` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- warranty_tickets
-- ------------------------------------------------------------
CREATE TABLE `warranty_tickets` (
  `ticket_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_item_id` bigint DEFAULT NULL,
  `imei_serial` varchar(100) NOT NULL,
  `phone_lookup` varchar(20) DEFAULT NULL,
  `issue_desc` tinytext,
  `status` enum('SUBMITTED','IN_PROGRESS','REPLACED_1_1','REPAIRED','CLOSED') DEFAULT 'SUBMITTED',
  `assigned_staff_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `resolved_at` datetime DEFAULT NULL,
  PRIMARY KEY (`ticket_id`),
  KEY `user_id` (`user_id`),
  KEY `order_item_id` (`order_item_id`),
  KEY `assigned_staff_id` (`assigned_staff_id`),
  CONSTRAINT `warranty_tickets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `warranty_tickets_ibfk_2` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`order_item_id`),
  CONSTRAINT `warranty_tickets_ibfk_3` FOREIGN KEY (`assigned_staff_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- blogs
-- ------------------------------------------------------------
CREATE TABLE `blogs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(100) DEFAULT NULL,
  `content` longtext NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `slug` varchar(255) NOT NULL,
  `status` enum('DRAFT','PUBLISHED','HIDDEN','REJECTED') NOT NULL,
  `summary` text,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `views_count` int NOT NULL,
  `author_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_pl5w1yw2c5lligoeb9a393fr3` (`slug`),
  KEY `idx_blog_slug` (`slug`),
  KEY `idx_blog_status` (`status`),
  KEY `idx_blog_category` (`category`),
  KEY `FKt8g0udj2fq40771g38t2t011n` (`author_id`),
  CONSTRAINT `FKt8g0udj2fq40771g38t2t011n` FOREIGN KEY (`author_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- contacts
-- ------------------------------------------------------------
CREATE TABLE `contacts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `message` text NOT NULL,
  `subject` varchar(150) NOT NULL,
  `replied_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','REPLIED','HIDDEN') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- otp_verification
-- ------------------------------------------------------------
CREATE TABLE `otp_verification` (
  `otp_id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `expired_at` datetime(6) NOT NULL,
  `otp_code` varchar(6) NOT NULL,
  `purpose` enum('REGISTER','RESET_PASSWORD','CHECKOUT') NOT NULL,
  `verified` bit(1) NOT NULL,
  PRIMARY KEY (`otp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- End of script. Only `roles` is seeded (required lookup data).
-- All other tables are empty and ready for the app to populate.
-- ============================================================