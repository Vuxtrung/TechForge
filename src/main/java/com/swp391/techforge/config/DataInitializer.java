package com.swp391.techforge.config;

import com.swp391.techforge.entity.*;
import com.swp391.techforge.repository.authentication.RoleRepository;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.category.CategoryRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotFound(1, "GUEST");
        createRoleIfNotFound(2, "CUSTOMER");
        createRoleIfNotFound(3, "STAFF_SALES");
        createRoleIfNotFound(4, "STAFF_WARRANTY");
        createRoleIfNotFound(5, "ADMIN");

        User adminUser = userRepository.findByEmail("admin@techforge.com").orElse(null);
        Role adminRole = roleRepository.findByRoleName("ADMIN").orElse(null);

        if (adminUser == null) {
            adminUser = new User();
            adminUser.setEmail("admin@techforge.com");
        }
        adminUser.setRole(adminRole);
        adminUser.setFullName("TechForge Admin");
        adminUser.setPasswordHash(passwordEncoder.encode("123456"));
        adminUser.setPhone("0988888888");
        adminUser.setAddress("Hà Nội");
        adminUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(adminUser);

        User customerUser = userRepository.findByEmail("customer@techforge.com").orElse(null);
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElse(null);

        if (customerUser == null) {
            customerUser = new User();
            customerUser.setEmail("customer@techforge.com");
        }
        customerUser.setRole(customerRole);
        customerUser.setFullName("Khách Hàng Mẫu");
        customerUser.setPasswordHash(passwordEncoder.encode("123456"));
        customerUser.setPhone("0977777777");
        customerUser.setAddress("Hà Nội");
        customerUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(customerUser);

        seedCategoriesAndProducts();
    }

    private void createRoleIfNotFound(Integer roleId, String roleName) {
        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isEmpty()) {
            Role role = new Role();
            role.setRoleId(roleId);
            role.setRoleName(roleName);
            roleRepository.save(role);
        }
    }

    private void seedCategoriesAndProducts() {
        if (categoryRepository.count() == 0) {
            Category catPc = new Category();
            catPc.setName("PC Gaming & Workstation");
            catPc.setType(Category.CategoryType.PC_PRODUCT);
            catPc.setActive(true);
            categoryRepository.save(catPc);

            Category catComponent = new Category();
            catComponent.setName("Linh Kiện Máy Tính");
            catComponent.setType(Category.CategoryType.PC_COMPONENT);
            catComponent.setActive(true);
            categoryRepository.save(catComponent);

            Category catGpu = new Category();
            catGpu.setName("GPU - Card Màn Hình");
            catGpu.setType(Category.CategoryType.PC_COMPONENT);
            catGpu.setParent(catComponent);
            catGpu.setActive(true);
            categoryRepository.save(catGpu);

            Category catCpu = new Category();
            catCpu.setName("CPU - Bộ Vi Xử Lý");
            catCpu.setType(Category.CategoryType.PC_COMPONENT);
            catCpu.setParent(catComponent);
            catCpu.setActive(true);
            categoryRepository.save(catCpu);
        }

        if (productRepository.count() == 0) {
            Category pcCategory = categoryRepository.findAll().stream()
                    .filter(c -> c.getType() == Category.CategoryType.PC_PRODUCT)
                    .findFirst().orElse(null);

            Category componentCategory = categoryRepository.findAll().stream()
                    .filter(c -> c.getType() == Category.CategoryType.PC_COMPONENT)
                    .findFirst().orElse(null);

            createProduct("PC TechForge Ultra Gaming i9-14900K / RTX 4090 24GB",
                    "Cấu hình gaming đồ họa đỉnh cao 2026, trang bị vi xử lý Intel i9 14900K, VGA ASUS ROG Strix RTX 4090 24GB, RAM 64GB DDR5, SSD 2TB Gen4.",
                    BigDecimal.valueOf(89990000), 15, "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800", pcCategory);

            createProduct("PC TechForge Vanguard i7-14700K / RTX 4070 Ti Super",
                    "Cấu hình gaming chuyên nghiệp 2K/4K, i7 14700K, RTX 4070 Ti Super 16GB, RAM 32GB DDR5, Tản nhiệt nước AIO 360mm.",
                    BigDecimal.valueOf(49990000), 25, "https://images.unsplash.com/photo-1591488320449-011701bb6704?w=800", pcCategory);

            createProduct("PC TechForge Streamer Ryzen 7 7800X3D / RTX 4070",
                    "Cấu hình livestream esport đỉnh cao, chip AMD Ryzen 7 7800X3D chuyên game, RTX 4070 12GB, RAM 32GB Bus 6000MHz.",
                    BigDecimal.valueOf(39990000), 30, "https://images.unsplash.com/photo-1547082297-819692d51857?w=800", pcCategory);

            createProduct("PC TechForge Office Pro i5-13400 / 16GB RAM / 512GB SSD",
                    "Máy tính văn phòng học tập doanh nghiệp siêu bền bỉ, Intel Core i5 13400, RAM 16GB, SSD NVMe 512GB siêu tốc.",
                    BigDecimal.valueOf(12990000), 50, "https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=800", pcCategory);

            createProduct("CPU Intel Core i9-14900K (Up to 6.0GHz, 24 Nhân 32 Luồng)",
                    "Bộ vi xử lý flagship thế hệ 14 của Intel, xung nhịp turbo lên tới 6.0GHz, xử lý đồ họa render 3D siêu tốc.",
                    BigDecimal.valueOf(15490000), 40, "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=800", componentCategory);

            createProduct("Card màn hình ASUS ROG Strix GeForce RTX 4090 OC 24GB",
                    "VGA cao cấp nhất kiến trúc Ada Lovelace, trang bị 24GB GDDR6X, hệ thống quạt Axial-tech mát lạnh.",
                    BigDecimal.valueOf(56990000), 10, "https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=800", componentCategory);

            createProduct("RAM Corsair Vengeance RGB 32GB (2x16GB) DDR5 6000MHz",
                    "Bộ nhớ trong DDR5 hiệu năng cao tích hợp dải đèn LED RGB sinh động, tương thích hoàn hảo XMP 3.0.",
                    BigDecimal.valueOf(3890000), 60, "https://images.unsplash.com/photo-1562976540-1502c2145186?w=800", componentCategory);

            createProduct("Ổ cứng SSD Samsung 990 PRO 1TB M.2 NVMe PCIe Gen 4.0",
                    "SSD PCIe 4.0 có tốc độ đọc/ghi hàng đầu thế giới 7450/6900 MB/s, tối ưu cho game thủ và chuyên gia render.",
                    BigDecimal.valueOf(2990000), 100, "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=800", componentCategory);
        }
    }

    private void createProduct(String name, String description, BigDecimal price, int stock, String imageUrl, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setBasePrice(price);
        p.setStockQuantity(stock);
        p.setCategory(category);
        p.setStatus(Product.ProductStatus.ACTIVE);
        Product saved = productRepository.save(p);

        ProductImage img = new ProductImage(saved, imageUrl, true);
        saved.getImages().add(img);
        productRepository.save(saved);
    }
}

