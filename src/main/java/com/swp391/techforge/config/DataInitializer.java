package com.swp391.techforge.config;

import com.swp391.techforge.entity.*;
import com.swp391.techforge.entity.component.*;
import com.swp391.techforge.repository.authentication.RoleRepository;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.category.CategoryRepository;
import com.swp391.techforge.repository.component.CaseComponentRepository;
import com.swp391.techforge.repository.component.CoolerRepository;
import com.swp391.techforge.repository.component.CpuRepository;
import com.swp391.techforge.repository.component.GpuRepository;
import com.swp391.techforge.repository.component.MainboardRepository;
import com.swp391.techforge.repository.component.PsuRepository;
import com.swp391.techforge.repository.component.RamRepository;
import com.swp391.techforge.repository.component.StorageRepository;
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
        private final CpuRepository cpuRepository;
        private final MainboardRepository mainboardRepository;
        private final RamRepository ramRepository;
        private final GpuRepository gpuRepository;
        private final PsuRepository psuRepository;
        private final CaseComponentRepository caseComponentRepository;
        private final CoolerRepository coolerRepository;
        private final StorageRepository storageRepository;
        private final com.swp391.techforge.repository.blog.BlogRepository blogRepository;

        public DataInitializer(UserRepository userRepository,
                        RoleRepository roleRepository,
                        CategoryRepository categoryRepository,
                        ProductRepository productRepository,
                        PasswordEncoder passwordEncoder,
                        CpuRepository cpuRepository,
                        MainboardRepository mainboardRepository,
                        RamRepository ramRepository,
                        GpuRepository gpuRepository,
                        PsuRepository psuRepository,
                        CaseComponentRepository caseComponentRepository,
                        CoolerRepository coolerRepository,
                        StorageRepository storageRepository,
                        com.swp391.techforge.repository.blog.BlogRepository blogRepository) {
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.categoryRepository = categoryRepository;
                this.productRepository = productRepository;
                this.passwordEncoder = passwordEncoder;
                this.cpuRepository = cpuRepository;
                this.mainboardRepository = mainboardRepository;
                this.ramRepository = ramRepository;
                this.gpuRepository = gpuRepository;
                this.psuRepository = psuRepository;
                this.caseComponentRepository = caseComponentRepository;
                this.coolerRepository = coolerRepository;
                this.storageRepository = storageRepository;
                this.blogRepository = blogRepository;
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

                Role staffSalesRole = roleRepository.findByRoleName("STAFF_SALES").orElse(null);
                User staffUser = userRepository.findByEmail("sales01@techforge.com").orElse(null);
                if (staffUser == null) {
                        staffUser = new User();
                        staffUser.setEmail("sales01@techforge.com");
                }
                staffUser.setRole(staffSalesRole);
                staffUser.setFullName("Nhân Viên Sales");
                staffUser.setPasswordHash(passwordEncoder.encode("123456"));
                staffUser.setPhone("0911111111");
                staffUser.setAddress("Hà Nội");
                staffUser.setStatus(UserStatus.ACTIVE);
                userRepository.save(staffUser);

                Role staffWarrantyRole = roleRepository.findByRoleName("STAFF_WARRANTY").orElse(null);
                User warrantyUser = userRepository.findByEmail("warranty01@techforge.com").orElse(null);
                if (warrantyUser == null) {
                        warrantyUser = new User();
                        warrantyUser.setEmail("warranty01@techforge.com");
                }
                warrantyUser.setRole(staffWarrantyRole);
                warrantyUser.setFullName("Nhân Viên Bảo Hành");
                warrantyUser.setPasswordHash(passwordEncoder.encode("123456")); 
                warrantyUser.setPhone("0922222222");
                warrantyUser.setAddress("Hà Nội");
                warrantyUser.setStatus(UserStatus.ACTIVE);
                userRepository.save(warrantyUser);

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
                seedSampleBlogs(adminUser);
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
                Category catPc = getOrCreateCategory("PC Gaming & Workstation",
                                Category.CategoryType.PC_PRODUCT, Category.ComponentType.NONE, null);
                Category catComponent = getOrCreateCategory("Linh Kiện Máy Tính",
                                Category.CategoryType.PC_COMPONENT, Category.ComponentType.NONE, null);

                // 8 category con dùng đúng tên mà Build PC (buildpc.js -> CATEGORIES[].key)
                // đang tìm bằng LIKE ở BuildPcApiController.getComponents(). Trước đây các
                // category này không tồn tại nên các tab RAM/Mainboard/Nguồn/Ổ Cứng/Fan tản
                // nhiệt/Vỏ máy luôn trống, còn CPU/VGA thì bị lệch vì sản phẩm lại gán vào
                // category cha "Linh Kiện Máy Tính" thay vì category con tương ứng.
                Category catCpu = getOrCreateCategory("CPU", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.CPU, catComponent);
                Category catMainboard = getOrCreateCategory("Mainboard", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.MAINBOARD, catComponent);
                Category catRam = getOrCreateCategory("RAM", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.RAM, catComponent);
                Category catVga = getOrCreateCategory("VGA", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.GPU, catComponent);
                Category catPsu = getOrCreateCategory("Nguồn", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.PSU, catComponent);
                Category catStorage = getOrCreateCategory("Ổ Cứng", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.STORAGE, catComponent);
                Category catCooler = getOrCreateCategory("Fan tản nhiệt", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.COOLER, catComponent);
                Category catCase = getOrCreateCategory("Vỏ máy", Category.CategoryType.PC_COMPONENT,
                                Category.ComponentType.CASE_TYPE, catComponent);

                if (productRepository.count() > 0) {
                        return;
                }

                createProduct("PC TechForge Ultra Gaming i9-14900K / RTX 4090 24GB",
                                "Cấu hình gaming đồ họa đỉnh cao 2026, trang bị vi xử lý Intel i9 14900K, VGA ASUS ROG Strix RTX 4090 24GB, RAM 64GB DDR5, SSD 2TB Gen4.",
                                BigDecimal.valueOf(89990000), 15,
                                "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800", catPc);

                createProduct("PC TechForge Vanguard i7-14700K / RTX 4070 Ti Super",
                                "Cấu hình gaming chuyên nghiệp 2K/4K, i7 14700K, RTX 4070 Ti Super 16GB, RAM 32GB DDR5, Tản nhiệt nước AIO 360mm.",
                                BigDecimal.valueOf(49990000), 25,
                                "https://images.unsplash.com/photo-1591488320449-011701bb6704?w=800", catPc);

                createProduct("PC TechForge Streamer Ryzen 7 7800X3D / RTX 4070",
                                "Cấu hình livestream esport đỉnh cao, chip AMD Ryzen 7 7800X3D chuyên game, RTX 4070 12GB, RAM 32GB Bus 6000MHz.",
                                BigDecimal.valueOf(39990000), 30,
                                "https://images.unsplash.com/photo-1547082297-819692d51857?w=800", catPc);

                createProduct("PC TechForge Office Pro i5-13400 / 16GB RAM / 512GB SSD",
                                "Máy tính văn phòng học tập doanh nghiệp siêu bền bỉ, Intel Core i5 13400, RAM 16GB, SSD NVMe 512GB siêu tốc.",
                                BigDecimal.valueOf(12990000), 50,
                                "https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=800", catPc);

                // ==== Linh kiện rời cho Build PC — mỗi sản phẩm kèm 1 row bảng con tương
                // ứng để CompatibilityService có dữ liệu thật để test đủ 7 rule ====

                Product cpu1 = createProduct("CPU Intel Core i9-14900K (Up to 6.0GHz, 24 Nhân 32 Luồng)",
                                "Bộ vi xử lý flagship thế hệ 14 của Intel, xung nhịp turbo lên tới 6.0GHz, xử lý đồ họa render 3D siêu tốc.",
                                BigDecimal.valueOf(15490000), 40,
                                "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=800", catCpu);
                saveCpu(cpu1, "LGA1700", 24, 32, 253);

                Product cpu2 = createProduct("CPU AMD Ryzen 7 7800X3D (8 Nhân 16 Luồng, 3D V-Cache)",
                                "Vi xử lý chuyên game hàng đầu nhờ công nghệ 3D V-Cache, hiệu năng chơi game vượt trội, tiết kiệm điện.",
                                BigDecimal.valueOf(9990000), 35,
                                "https://images.unsplash.com/photo-1555617981-dac3880eac6e?w=800", catCpu);
                saveCpu(cpu2, "AM5", 8, 16, 120);

                Product mb1 = createProduct("Mainboard ASUS ROG Strix Z790-E Gaming WiFi",
                                "Bo mạch chủ cao cấp chipset Z790 cho CPU Intel LGA1700, hỗ trợ DDR5, PCIe 5.0, WiFi 6E tích hợp.",
                                BigDecimal.valueOf(9990000), 20,
                                "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800", catMainboard);
                saveMainboard(mb1, "LGA1700", "DDR5", 4, 192, "ATX");

                Product mb2 = createProduct("Mainboard MSI MAG B650 Tomahawk WiFi",
                                "Bo mạch chủ chipset B650 cho CPU AMD AM5, hỗ trợ DDR5, PCIe 4.0, VRM tản nhiệt tốt cho ép xung.",
                                BigDecimal.valueOf(5490000), 25,
                                "https://images.unsplash.com/photo-1591405351990-4726e331f141?w=800", catMainboard);
                saveMainboard(mb2, "AM5", "DDR5", 4, 128, "ATX");

                Product ram1 = createProduct("RAM Corsair Vengeance RGB 32GB (2x16GB) DDR5 6000MHz",
                                "Bộ nhớ trong DDR5 hiệu năng cao tích hợp dải đèn LED RGB sinh động, tương thích hoàn hảo XMP 3.0.",
                                BigDecimal.valueOf(3890000), 60,
                                "https://images.unsplash.com/photo-1562976540-1502c2145186?w=800", catRam);
                saveRam(ram1, "DDR5", 6000, 16, 2);

                Product gpu1 = createProduct("Card màn hình ASUS ROG Strix GeForce RTX 4090 OC 24GB",
                                "VGA cao cấp nhất kiến trúc Ada Lovelace, trang bị 24GB GDDR6X, hệ thống quạt Axial-tech mát lạnh.",
                                BigDecimal.valueOf(56990000), 10,
                                "https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=800", catVga);
                saveGpu(gpu1, 24, 348, "16-pin", 1000, 3);

                Product gpu2 = createProduct("Card màn hình MSI Gaming X Trio RTX 4070 12GB",
                                "VGA tầm trung cao cấp cho gaming 2K, hiệu năng/giá tốt, hệ thống tản nhiệt Tri Frozr 3 êm ái.",
                                BigDecimal.valueOf(16990000), 18,
                                "https://images.unsplash.com/photo-1591488320449-011701bb6704?w=800", catVga);
                saveGpu(gpu2, 12, 337, "8-pin", 650, 2);

                Product psu1 = createProduct("Nguồn Corsair RM1000e 1000W 80 Plus Gold Full Modular",
                                "PSU công suất lớn, chuẩn 80 Plus Gold, dây rời hoàn toàn, đủ tải cho cấu hình RTX 4090.",
                                BigDecimal.valueOf(3490000), 30,
                                "https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=800", catPsu);
                savePsu(psu1, 1000, "80 Plus Gold", "FULL", "ATX");

                Product psu2 = createProduct("Nguồn Cooler Master MWE 650W 80 Plus Bronze",
                                "PSU phổ thông ổn định, phù hợp cấu hình văn phòng/gaming tầm trung.",
                                BigDecimal.valueOf(1290000), 45,
                                "https://images.unsplash.com/photo-1591370874773-6702e8f12fd8?w=800", catPsu);
                savePsu(psu2, 650, "80 Plus Bronze", "NONE", "ATX");

                Product storage1 = createProduct("Ổ cứng SSD Samsung 990 PRO 1TB M.2 NVMe PCIe Gen 4.0",
                                "SSD PCIe 4.0 có tốc độ đọc/ghi hàng đầu thế giới 7450/6900 MB/s, tối ưu cho game thủ và chuyên gia render.",
                                BigDecimal.valueOf(2990000), 100,
                                "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=800", catStorage);
                saveStorage(storage1, "SSD_NVME", "NVMe PCIe 4.0", 1000);

                Product cooler1 = createProduct("Tản nhiệt nước AIO NZXT Kraken 360mm RGB",
                                "Tản nhiệt AIO 360mm làm mát mạnh mẽ cho CPU cao cấp, đèn RGB đồng bộ, hỗ trợ đa socket Intel/AMD.",
                                BigDecimal.valueOf(4290000), 15,
                                "https://images.unsplash.com/photo-1591405351990-4726e331f141?w=800", catCooler);
                saveCooler(cooler1, "AIO", null, 360, "LGA1700,AM5,AM4");

                Product cooler2 = createProduct("Tản nhiệt khí DeepCool AK620 Dual Tower",
                                "Tản khí dual tower 6 heatpipe, hiệu năng tương đương AIO 240mm, giá thành hợp lý.",
                                BigDecimal.valueOf(1190000), 40,
                                "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800", catCooler);
                saveCooler(cooler2, "AIR", 160, null, "LGA1700,AM5,AM4");

                Product case1 = createProduct("Vỏ máy Lian Li O11 Dynamic EVO Mid Tower",
                                "Case gaming cao cấp hỗ trợ ATX/M-ATX/ITX, không gian rộng cho tản AIO 360mm và GPU dài.",
                                BigDecimal.valueOf(3290000), 20,
                                "https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=800", catCase);
                saveCase(case1, "ATX,MICRO_ATX,MINI_ITX", 420, 175, 360);
        }

        private Category getOrCreateCategory(String name, Category.CategoryType type,
                        Category.ComponentType componentType, Category parent) {
                return categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                                .filter(c -> c.getName().equalsIgnoreCase(name))
                                .findFirst()
                                .orElseGet(() -> {
                                        Category c = new Category();
                                        c.setName(name);
                                        c.setType(type);
                                        c.setComponentType(componentType);
                                        c.setParent(parent);
                                        c.setActive(true);
                                        return categoryRepository.save(c);
                                });
        }

        private Product createProduct(String name, String description, BigDecimal price, int stock, String imageUrl,
                        Category category) {
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
                return productRepository.save(saved);
        }

        private void saveCpu(Product product, String socket, int cores, int threads, int tdpWatt) {
                Cpu cpu = new Cpu();
                cpu.setProduct(product);
                cpu.setSocket(socket);
                cpu.setCores(cores);
                cpu.setThreads(threads);
                cpu.setTdpWatt(tdpWatt);
                cpu.setHasIgpu(false);
                cpuRepository.save(cpu);
        }

        private void saveMainboard(Product product, String socket, String ramType, int ramSlots, int maxRamGb,
                        String formFactor) {
                Mainboard mb = new Mainboard();
                mb.setProduct(product);
                mb.setSocket(socket);
                mb.setRamType(Mainboard.RamType.valueOf(ramType));
                mb.setRamSlots(ramSlots);
                mb.setMaxRamGb(maxRamGb);
                mb.setFormFactor(Mainboard.FormFactor.valueOf(formFactor));
                mainboardRepository.save(mb);
        }

        private void saveRam(Product product, String ramType, int speedMhz, int capacityGbPerModule, int modules) {
                Ram ram = new Ram();
                ram.setProduct(product);
                ram.setRamType(Ram.RamType.valueOf(ramType));
                ram.setSpeedMhz(speedMhz);
                ram.setCapacityGb(capacityGbPerModule);
                ram.setModules(modules);
                ramRepository.save(ram);
        }

        private void saveGpu(Product product, int vramGb, int lengthMm, String powerConnector, int recommendedPsuWatt,
                        int slotWidth) {
                Gpu gpu = new Gpu();
                gpu.setProduct(product);
                gpu.setVramGb(vramGb);
                gpu.setLengthMm(lengthMm);
                gpu.setPowerConnector(powerConnector);
                gpu.setRecommendedPsuWatt(recommendedPsuWatt);
                gpu.setSlotWidth(slotWidth);
                gpuRepository.save(gpu);
        }

        private void savePsu(Product product, int wattage, String efficiencyRating, String modular, String formFactor) {
                Psu psu = new Psu();
                psu.setProduct(product);
                psu.setWattage(wattage);
                psu.setEfficiencyRating(efficiencyRating);
                psu.setModular(Psu.Modular.valueOf(modular));
                psu.setFormFactor(formFactor);
                psuRepository.save(psu);
        }

        private void saveStorage(Product product, String storageType, String storageInterface, int capacityGb) {
                Storage storage = new Storage();
                storage.setProduct(product);
                storage.setStorageType(Storage.StorageType.valueOf(storageType));
                storage.setStorageInterface(storageInterface);
                storage.setCapacityGb(capacityGb);
                storageRepository.save(storage);
        }

        private void saveCooler(Product product, String coolerType, Integer heightMm, Integer radiatorSizeMm,
                        String socketSupport) {
                Cooler cooler = new Cooler();
                cooler.setProduct(product);
                cooler.setCoolerType(Cooler.CoolerType.valueOf(coolerType));
                cooler.setHeightMm(heightMm);
                cooler.setRadiatorSizeMm(radiatorSizeMm);
                cooler.setSocketSupport(socketSupport);
                coolerRepository.save(cooler);
        }

        private void saveCase(Product product, String formFactorSupport, int maxGpuLengthMm, int maxCoolerHeightMm,
                        int maxRadiatorMm) {
                CaseComponent c = new CaseComponent();
                c.setProduct(product);
                c.setFormFactorSupport(formFactorSupport);
                c.setMaxGpuLengthMm(maxGpuLengthMm);
                c.setMaxCoolerHeightMm(maxCoolerHeightMm);
                c.setMaxRadiatorMm(maxRadiatorMm);
                caseComponentRepository.save(c);
        }

        private void seedSampleBlogs(User adminUser) {
                if (blogRepository.count() > 0) {
                        return;
                }

                com.swp391.techforge.entity.Blog b1 = new com.swp391.techforge.entity.Blog();
                b1.setTitle("Top 5 Cấu Hình PC Gaming Đáng Mua Nhất 2026 Phân Khúc Từ 15 Đến 40 Triệu");
                b1.setSlug("top-5-cau-hinh-pc-gaming-dang-mua-nhat-2026");
                b1.setCategory("Hướng Dẫn Build PC");
                b1.setSummary("Tổng hợp 5 bộ cấu hình PC Gaming tối ưu hiệu năng trên giá thành (P/P) tốt nhất năm 2026, chiến mượt mọi tựa game AAA từ Full HD đến 4K.");
                b1.setContent("<p>Năm 2026 chứng kiến sự bùng nổ của các thế hệ vi xử lý và card đồ họa mới. Để giúp các game thủ dễ dàng lựa chọn cấu hình phù hợp với túi tiền, TechForge xin gửi đến bạn bảng phân tích chi tiết 5 cấu hình hot nhất:</p>"
                                + "<h3>1. Cấu hình Esports 15 Triệu: Core i5 + RTX 4060</h3>"
                                + "<p>Đây là cấu hình hoàn hảo cho các tựa game như LMHT, CS2, Valorant và GTA V ở độ phân giải 1080p với mức FPS trên 144.</p>"
                                + "<h3>2. Cấu hình AAA 25 Triệu: Ryzen 5 7600X + RTX 4070 Super</h3>"
                                + "<p>Trang bị chuẩn RAM DDR5 tốc độ cao và kiến trúc Zen 4 mạnh mẽ, giúp bạn trải nghiệm Cyberpunk 2077 và Black Myth: Wukong mượt mà ở mức thiết lập đồ họa cao.</p>"
                                + "<h3>3. Cấu hình Đồ Họa & Streamer 40 Triệu: Core i7 14700K + RTX 4080</h3>"
                                + "<p>Đa nhiệm vượt trội với 20 nhân 28 luồng, render 3D, dựng phim 4K và livestream mượt mà không bị tụt khung hình.</p>");
                b1.setThumbnailUrl("https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800");
                b1.setStatus(com.swp391.techforge.entity.BlogStatus.PUBLISHED);
                b1.setAuthor(adminUser);
                b1.setViewsCount(1540);
                blogRepository.save(b1);

                com.swp391.techforge.entity.Blog b2 = new com.swp391.techforge.entity.Blog();
                b2.setTitle("Đánh Giá Chi Tiết Intel Core i9-14900K vs AMD Ryzen 9 7950X3D: Ai Là Vua Gaming?");
                b2.setSlug("danh-gia-intel-core-i9-14900k-vs-amd-ryzen-9-7950x3d");
                b2.setCategory("Đánh Giá Phần Cứng");
                b2.setSummary("So sánh điểm benchmark chi tiết, nhiệt độ, mức tiêu thụ điện năng và trải nghiệm chơi game thực tế giữa hai con chip CPU đầu bảng hiện nay.");
                b2.setContent("<p>Cuộc chiến giữa đội Xanh (Intel) và đội Đỏ (AMD) chưa bao giờ hạ nhiệt. Hôm nay hãy cùng TechForge so sánh trực tiếp hiệu năng giữa <strong>Core i9-14900K</strong> và <strong>Ryzen 9 7950X3D</strong>.</p>"
                                + "<h3>Hiệu năng chơi game</h3>"
                                + "<p>Nhờ công nghệ 3D V-Cache đột phá, Ryzen 9 7950X3D cho mức FPS trung bình nhỉnh hơn khoảng 5-8% trong các tựa game nặng về cache như Microsoft Flight Simulator và Baldur's Gate 3.</p>"
                                + "<h3>Nhiệt độ và điện năng tiêu thụ</h3>"
                                + "<p>AMD vượt trội về khả năng tiết kiệm điện với TDP chỉ 120W, trong khi Intel i9 có thể ăn tới 253W+ khi mở toàn bộ turbo limit.</p>");
                b2.setThumbnailUrl("https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=800");
                b2.setStatus(com.swp391.techforge.entity.BlogStatus.PUBLISHED);
                b2.setAuthor(adminUser);
                b2.setViewsCount(2380);
                blogRepository.save(b2);

                com.swp391.techforge.entity.Blog b3 = new com.swp391.techforge.entity.Blog();
                b3.setTitle("Hướng Dẫn Tự Lắp Ráp PC Gaming Chuẩn Từng Bước Cho Người Mới Bắt Đầu");
                b3.setSlug("huong-dan-tu-lap-rap-pc-gaming-chuan-tung-step");
                b3.setCategory("Hướng Dẫn Build PC");
                b3.setSummary("Cẩm nang tự lắp ráp linh kiện máy tính tại nhà không sợ chập cháy, từ cách lắp CPU, tra keo tản nhiệt đến đi dây nguồn gọn gàng.");
                b3.setContent("<p>Tự tay lắp ráp một cỗ máy PC mang lại trải nghiệm vô cùng thú vị và tự hào. Dưới đây là các bước quan trọng bạn cần nắm rõ:</p>"
                                + "<ol>"
                                + "<li><strong>Bước 1: Chuẩn bị Mainboard và lắp CPU</strong> - Chú ý góc tam giác vàng trên CPU để đặt đúng chiều socket.</li>"
                                + "<li><strong>Bước 2: Lắp RAM và ổ cứng SSD M.2</strong> - Nhớ cắm RAM vào khe 2 và 4 để kích hoạt Dual Channel.</li>"
                                + "<li><strong>Bước 3: Lắp nguồn và mainboard vào vỏ case</strong> - Vặn chặt các chân ốc standoff trước khi siết vít.</li>"
                                + "<li><strong>Bước 4: Cắm card đồ họa và kết nối dây nguồn PCIe/12VHPWR.</strong></li>"
                                + "</ol>");
                b3.setThumbnailUrl("https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=800");
                b3.setStatus(com.swp391.techforge.entity.BlogStatus.PUBLISHED);
                b3.setAuthor(adminUser);
                b3.setViewsCount(3410);
                blogRepository.save(b3);

                com.swp391.techforge.entity.Blog b4 = new com.swp391.techforge.entity.Blog();
                b4.setTitle("Chương Trình Khuyến Mãi Mùa Hè TechForge: Giảm Giá Linh Kiện Đến 50%");
                b4.setSlug("chuong-trinh-khuyen-mai-mua-he-techforge-2026");
                b4.setCategory("Khuyến Mãi & Sự Kiện");
                b4.setSummary("Săn voucher giảm giá hấp dẫn, tặng kèm bàn phím cơ gaming và hỗ trợ trả góp 0% lãi suất khi mua trọn bộ PC tại TechForge.");
                b4.setContent("<p>Chào đón mùa hè 2026, TechForge trân trọng mang đến chương trình tri ân khách hàng đặc biệt với hàng loạt ưu đãi khủng:</p>"
                                + "<ul>"
                                + "<li>Giảm ngay 2.000.000đ khi build trọn bộ PC từ 20 triệu đồng.</li>"
                                + "<li>Tặng kèm chuột gaming không dây và lót chuột RGB cao cấp.</li>"
                                + "<li>Bảo hành 1 đổi 1 tận nơi trong 30 ngày đầu tiên.</li>"
                                + "</ul>");
                b4.setThumbnailUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800");
                b4.setStatus(com.swp391.techforge.entity.BlogStatus.PUBLISHED);
                b4.setAuthor(adminUser);
                b4.setViewsCount(890);
                blogRepository.save(b4);
        }
}