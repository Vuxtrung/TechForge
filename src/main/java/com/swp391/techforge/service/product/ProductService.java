package com.swp391.techforge.service.product;

import com.swp391.techforge.dto.product.ProductFilterOptions;
import com.swp391.techforge.dto.product.ProductFilterRequest;
import com.swp391.techforge.dto.product.ComponentSpecRequest;
import com.swp391.techforge.entity.Category;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.ProductImage;
import com.swp391.techforge.entity.component.*;
import com.swp391.techforge.repository.category.CategoryRepository;
import com.swp391.techforge.repository.component.*;
import com.swp391.techforge.repository.product.ProductImageRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.swp391.techforge.entity.ProductSpecification;
import com.swp391.techforge.repository.product.ProductSpecificationRepository;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class ProductService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductSpecificationRepository productSpecificationRepository;
    private final CpuRepository cpuRepository;
    private final MainboardRepository mainboardRepository;
    private final RamRepository ramRepository;
    private final GpuRepository gpuRepository;
    private final PsuRepository psuRepository;
    private final CaseComponentRepository caseRepository;
    private final CoolerRepository coolerRepository;
    private final StorageRepository storageRepository;

    public ProductService(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository productImageRepository,
            ProductSpecificationRepository productSpecificationRepository,
            CloudinaryService cloudinaryService,
            CpuRepository cpuRepository,
            MainboardRepository mainboardRepository,
            RamRepository ramRepository,
            GpuRepository gpuRepository,
            PsuRepository psuRepository,
            CaseComponentRepository caseRepository,
            CoolerRepository coolerRepository,
            StorageRepository storageRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.productSpecificationRepository = productSpecificationRepository;
        this.cloudinaryService = cloudinaryService;
        this.cpuRepository = cpuRepository;
        this.mainboardRepository = mainboardRepository;
        this.ramRepository = ramRepository;
        this.gpuRepository = gpuRepository;
        this.psuRepository = psuRepository;
        this.caseRepository = caseRepository;
        this.coolerRepository = coolerRepository;
        this.storageRepository = storageRepository;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String keyword, Long categoryId, String status,
            int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> result = productRepository.search(keyword, categoryId, status, pageable);
        result.forEach(p -> p.setCategoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null));
        return result;
    }

    // Dùng cho trang khách /products (F_07): map sort string -> Sort thật,
    // gọi searchPublic() (chỉ lấy ACTIVE), gán lại categoryId để View hiển thị đúng
    @Transactional(readOnly = true)
    public Page<Product> searchPublic(ProductFilterRequest filter) {
        Sort sort = switch (filter.getSort()) {
            case "priceAsc" ->
                Sort.by("basePrice").ascending();
            case "priceDesc" ->
                Sort.by("basePrice").descending();
            default ->
                Sort.by("createdAt").descending(); // "newest"
        };
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        // Nếu có lọc theo category, lấy luôn cả danh mục con cháu (đệ quy) để
        // bấm vào category cha (VD: "CPU - Vi Xử Lý") vẫn ra sản phẩm nằm ở category con
        List<Long> categoryIds = filter.getCategoryId() != null
                ? categoryRepository.findSelfAndDescendantIds(filter.getCategoryId())
                : null;

        Page<Product> result = productRepository.searchPublic(
                filter.getKeyword(),
                categoryIds,
                filter.getType(),
                filter.getBrand(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                pageable);

        result.forEach(p -> p.setCategoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null));
        return result;
    }

    // Dữ liệu cho sidebar filter: danh mục cha (kèm con) đang active + toàn bộ thương hiệu đang có sản phẩm
    @Transactional(readOnly = true)
    public ProductFilterOptions getFilterOptions() {
        List<Category> categories = categoryRepository.findAllByParentIsNullAndActiveTrueOrderByNameAsc();
        List<String> brands = productRepository.findDistinctBrands();
        return new ProductFilterOptions(categories, brands);
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        product.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null);
        return product;
    }

    // Dựng lại ComponentSpecRequest từ dữ liệu đã lưu (product_specifications
    // hoặc bảng linh kiện tương ứng), dùng để pre-fill form khi vào trang Sửa.
    @Transactional(readOnly = true)
    public ComponentSpecRequest getSpecRequestForEdit(Product product) {
        ComponentSpecRequest req = new ComponentSpecRequest();
        Category category = product.getCategory();
        if (category == null) {
            return req;
        }

        if (category.getType() == Category.CategoryType.PC_PRODUCT) {
            Map<String, String> specs = new LinkedHashMap<>();
            for (ProductSpecification s : productSpecificationRepository.findAllByProduct_ProductId(product.getProductId())) {
                specs.put(s.getSpecKey(), s.getSpecValue());
            }
            req.setPcCpu(specs.get("CPU"));
            req.setPcMainboard(specs.get("Mainboard"));
            req.setPcRam(specs.get("RAM"));
            req.setPcVga(specs.get("VGA"));
            req.setPcStorage(specs.get("Ổ cứng"));
            req.setPcPsu(specs.get("Nguồn"));
            req.setPcCooler(specs.get("Tản nhiệt"));
            req.setPcCase(specs.get("Case"));
            return req;
        }

        Category.ComponentType componentType = category.getComponentType();
        if (componentType == null) {
            return req;
        }

        switch (componentType) {
            case CPU -> cpuRepository.findById(product.getProductId()).ifPresent(c -> {
                req.setSocket(c.getSocket());
                req.setCores(c.getCores());
                req.setThreads(c.getThreads());
                req.setBaseClockGhz(c.getBaseClockGhz());
                req.setBoostClockGhz(c.getBoostClockGhz());
                req.setTdpWatt(c.getTdpWatt());
                req.setHasIgpu(c.getHasIgpu());
            });
            case MAINBOARD -> mainboardRepository.findById(product.getProductId()).ifPresent(mb -> {
                req.setSocket(mb.getSocket());
                req.setChipset(mb.getChipset());
                req.setMbRamType(mb.getRamType() != null ? mb.getRamType().name() : null);
                req.setRamSlots(mb.getRamSlots());
                req.setMaxRamGb(mb.getMaxRamGb());
                req.setMbFormFactor(mb.getFormFactor() != null ? mb.getFormFactor().name() : null);
                req.setM2Slots(mb.getM2Slots());
            });
            case RAM -> ramRepository.findById(product.getProductId()).ifPresent(ram -> {
                req.setRamType(ram.getRamType() != null ? ram.getRamType().name() : null);
                req.setSpeedMhz(ram.getSpeedMhz());
                req.setRamCapacityGb(ram.getCapacityGb());
                req.setModules(ram.getModules());
            });
            case GPU -> gpuRepository.findById(product.getProductId()).ifPresent(gpu -> {
                req.setVramGb(gpu.getVramGb());
                req.setLengthMm(gpu.getLengthMm());
                req.setPowerConnector(gpu.getPowerConnector());
                req.setRecommendedPsuWatt(gpu.getRecommendedPsuWatt());
                req.setSlotWidth(gpu.getSlotWidth());
            });
            case PSU -> psuRepository.findById(product.getProductId()).ifPresent(psu -> {
                req.setWattage(psu.getWattage());
                req.setEfficiencyRating(psu.getEfficiencyRating());
                req.setModular(psu.getModular() != null ? psu.getModular().name() : null);
                req.setPsuFormFactor(psu.getFormFactor());
            });
            case CASE_TYPE -> caseRepository.findById(product.getProductId()).ifPresent(c -> {
                req.setCaseFormFactorSupport(c.getFormFactorSupport() != null
                        ? new ArrayList<>(List.of(c.getFormFactorSupport().split(",")))
                        : new ArrayList<>());
                req.setMaxGpuLengthMm(c.getMaxGpuLengthMm());
                req.setMaxCoolerHeightMm(c.getMaxCoolerHeightMm());
                req.setMaxRadiatorMm(c.getMaxRadiatorMm());
            });
            case COOLER -> coolerRepository.findById(product.getProductId()).ifPresent(c -> {
                req.setCoolerType(c.getCoolerType() != null ? c.getCoolerType().name() : null);
                req.setHeightMm(c.getHeightMm());
                req.setRadiatorSizeMm(c.getRadiatorSizeMm());
                req.setSocketSupport(c.getSocketSupport());
            });
            case STORAGE -> storageRepository.findById(product.getProductId()).ifPresent(s -> {
                req.setStorageType(s.getStorageType() != null ? s.getStorageType().name() : null);
                req.setStorageInterface(s.getStorageInterface());
                req.setStorageCapacityGb(s.getCapacityGb());
            });
            case NONE -> { /* không có gì để nạp */ }
        }
        return req;
    }

    @Transactional
    public Product create(Product product, MultipartFile imageFile, ComponentSpecRequest specRequest) {
        validateName(product.getName(), null);
        Category category = resolveCategory(product.getCategoryId());
        product.setCategory(category);
        Product saved = productRepository.save(product);

        if (imageFile != null && !imageFile.isEmpty()) {
            uploadAndAttachImage(saved, imageFile, true);
        }
        saveSpecOrComponent(saved, category, specRequest);
        return saved;
    }

    @Transactional
    public Product update(Long id, Product incoming, MultipartFile imageFile, ComponentSpecRequest specRequest) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

        validateName(incoming.getName(), id);

        existing.setName(incoming.getName());
        existing.setBrand(incoming.getBrand());
        existing.setDescription(incoming.getDescription());
        existing.setBasePrice(incoming.getBasePrice());
        existing.setStockQuantity(incoming.getStockQuantity());
        existing.setStatus(incoming.getStatus());
        Category category = resolveCategory(incoming.getCategoryId());
        existing.setCategory(category);
        existing.setUpdatedAt(LocalDateTime.now());

        if (imageFile != null && !imageFile.isEmpty()) {
            // Xóa tất cả ảnh cũ
            productImageRepository.deleteByProduct_ProductId(existing.getProductId());
            // Upload ảnh mới là primary
            uploadAndAttachImage(existing, imageFile, true);
        }
        Product saved = productRepository.save(existing);
        saveSpecOrComponent(saved, category, specRequest);
        return saved;
    }

    private void uploadAndAttachImage(Product product, MultipartFile imageFile, boolean isPrimary) {
        validateImage(imageFile);
        try {
            String url = cloudinaryService.uploadImage(imageFile);
            ProductImage image = new ProductImage(product, url, isPrimary);
            productImageRepository.save(image);
        } catch (IOException e) {
            throw new IllegalArgumentException("Tải ảnh lên thất bại: " + e.getMessage());
        }
    }

    private void validateImage(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPG, PNG hoặc WEBP.");
        }
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Dung lượng ảnh vượt quá giới hạn cho phép (5MB).");
        }
    }

    // Tách theo category: PC_PRODUCT -> product_specifications (8 key cố định);
    // PC_COMPONENT -> đúng bảng linh kiện tương ứng componentType. Luôn xoá dữ
    // liệu spec cũ (cả 2 nguồn) trước khi lưu, để đổi category không để sót
    // rác từ loại cũ.
    private void saveSpecOrComponent(Product product, Category category, ComponentSpecRequest req) {
        productSpecificationRepository.deleteAllByProduct_ProductId(product.getProductId());
        deleteExistingComponentRows(product.getProductId());

        if (req == null) {
            return;
        }

        if (category.getType() == Category.CategoryType.PC_PRODUCT) {
            savePcProductSpecs(product, req);
            return;
        }

        Category.ComponentType componentType = category.getComponentType();
        if (componentType == null) {
            return;
        }

        switch (componentType) {
            case CPU -> saveCpu(product, req);
            case MAINBOARD -> saveMainboard(product, req);
            case RAM -> saveRam(product, req);
            case GPU -> saveGpu(product, req);
            case PSU -> savePsu(product, req);
            case CASE_TYPE -> saveCase(product, req);
            case COOLER -> saveCooler(product, req);
            case STORAGE -> saveStorage(product, req);
            case NONE -> { /* không cần lưu gì thêm */ }
        }
    }

    // Xoá sạch row linh kiện cũ ở TẤT CẢ 8 bảng cho product này (an toàn khi
    // admin đổi category từ loại linh kiện này sang loại khác) - mỗi bảng chỉ
    // có tối đa 1 row nên xoá theo productId là đủ, không cần biết loại cũ.
    private void deleteExistingComponentRows(Long productId) {
        cpuRepository.deleteByProductId(productId);
        mainboardRepository.deleteByProductId(productId);
        ramRepository.deleteByProductId(productId);
        gpuRepository.deleteByProductId(productId);
        psuRepository.deleteByProductId(productId);
        caseRepository.deleteByProductId(productId);
        coolerRepository.deleteByProductId(productId);
        storageRepository.deleteByProductId(productId);
    }

    private void savePcProductSpecs(Product product, ComponentSpecRequest req) {
        addSpecIfPresent(product, "CPU", req.getPcCpu());
        addSpecIfPresent(product, "Mainboard", req.getPcMainboard());
        addSpecIfPresent(product, "RAM", req.getPcRam());
        addSpecIfPresent(product, "VGA", req.getPcVga());
        addSpecIfPresent(product, "Ổ cứng", req.getPcStorage());
        addSpecIfPresent(product, "Nguồn", req.getPcPsu());
        addSpecIfPresent(product, "Tản nhiệt", req.getPcCooler());
        addSpecIfPresent(product, "Case", req.getPcCase());
    }

    private static final int PC_SPEC_VALUE_MAX_LENGTH = 255; // khớp product_specifications.spec_value VARCHAR(255)

    private void addSpecIfPresent(Product product, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() > PC_SPEC_VALUE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Thông số \"" + key + "\" không được vượt quá " + PC_SPEC_VALUE_MAX_LENGTH + " ký tự.");
        }
        ProductSpecification spec = new ProductSpecification();
        spec.setProduct(product);
        spec.setSpecKey(key);
        spec.setSpecValue(trimmed);
        productSpecificationRepository.save(spec);
    }

    // ==== Helper validate dùng chung cho các bảng linh kiện ====

    // Parse enum an toàn: chuyển IllegalArgumentException kỹ thuật của
    // Enum.valueOf() (VD: "No enum constant ...") thành thông báo tiếng Việt dễ hiểu.
    private <E extends Enum<E>> E parseRequiredEnum(Class<E> enumClass, String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn " + fieldLabel + ".");
        }
        try {
            return Enum.valueOf(enumClass, raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Giá trị " + fieldLabel + " không hợp lệ.");
        }
    }

    // Parse enum có giá trị mặc định khi bỏ trống (VD: Modular, Form Factor PSU)
    private <E extends Enum<E>> E parseOptionalEnum(Class<E> enumClass, String raw, E defaultValue, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Giá trị " + fieldLabel + " không hợp lệ.");
        }
    }

    // Số nguyên: bắt buộc >= min (min=0 cho phép 0, min=1 bắt buộc dương)
    private void requireIntMin(Integer value, int min, String fieldLabel) {
        if (value != null && value < min) {
            throw new IllegalArgumentException(fieldLabel + " phải >= " + min + ".");
        }
    }

    private void requireDecimalPositive(BigDecimal value, String fieldLabel) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldLabel + " phải lớn hơn 0.");
        }
    }

    private void requireLength(String value, int maxLength, String fieldLabel) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + " không được vượt quá " + maxLength + " ký tự.");
        }
    }

    private void saveCpu(Product product, ComponentSpecRequest req) {
        if (req.getSocket() == null || req.getSocket().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập Socket cho CPU.");
        }
        requireLength(req.getSocket(), 30, "Socket");
        requireIntMin(req.getCores(), 1, "Số nhân (Cores)");
        requireIntMin(req.getThreads(), 1, "Số luồng (Threads)");
        requireDecimalPositive(req.getBaseClockGhz(), "Xung nhịp cơ bản");
        requireDecimalPositive(req.getBoostClockGhz(), "Xung nhịp boost");
        requireIntMin(req.getTdpWatt(), 0, "TDP");

        Cpu cpu = new Cpu();
        cpu.setProduct(product);
        cpu.setSocket(req.getSocket().trim());
        cpu.setCores(req.getCores());
        cpu.setThreads(req.getThreads());
        cpu.setBaseClockGhz(req.getBaseClockGhz());
        cpu.setBoostClockGhz(req.getBoostClockGhz());
        cpu.setTdpWatt(req.getTdpWatt());
        cpu.setHasIgpu(Boolean.TRUE.equals(req.getHasIgpu()));
        cpuRepository.save(cpu);
    }

    private void saveMainboard(Product product, ComponentSpecRequest req) {
        if (req.getSocket() == null || req.getSocket().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập Socket cho Mainboard.");
        }
        requireLength(req.getSocket(), 30, "Socket");
        requireLength(req.getChipset(), 30, "Chipset");
        requireIntMin(req.getRamSlots(), 1, "Số khe RAM");
        requireIntMin(req.getMaxRamGb(), 1, "RAM tối đa");
        requireIntMin(req.getM2Slots(), 0, "Số khe M.2");

        Mainboard.RamType ramType = parseRequiredEnum(Mainboard.RamType.class, req.getMbRamType(), "Loại RAM cho Mainboard");
        Mainboard.FormFactor formFactor = parseRequiredEnum(Mainboard.FormFactor.class, req.getMbFormFactor(), "Form Factor cho Mainboard");

        Mainboard mb = new Mainboard();
        mb.setProduct(product);
        mb.setSocket(req.getSocket().trim());
        mb.setChipset(req.getChipset());
        mb.setRamType(ramType);
        mb.setRamSlots(req.getRamSlots());
        mb.setMaxRamGb(req.getMaxRamGb());
        mb.setFormFactor(formFactor);
        mb.setM2Slots(req.getM2Slots());
        mainboardRepository.save(mb);
    }

    private void saveRam(Product product, ComponentSpecRequest req) {
        if (req.getSpeedMhz() == null || req.getRamCapacityGb() == null) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ Bus và Dung lượng cho RAM.");
        }
        requireIntMin(req.getSpeedMhz(), 1, "Bus (MHz)");
        requireIntMin(req.getRamCapacityGb(), 1, "Dung lượng RAM");
        requireIntMin(req.getModules(), 1, "Số thanh RAM (Modules)");

        Ram.RamType ramType = parseRequiredEnum(Ram.RamType.class, req.getRamType(), "Loại RAM");

        Ram ram = new Ram();
        ram.setProduct(product);
        ram.setRamType(ramType);
        ram.setSpeedMhz(req.getSpeedMhz());
        ram.setCapacityGb(req.getRamCapacityGb());
        ram.setModules(req.getModules() != null ? req.getModules() : 1);
        ramRepository.save(ram);
    }

    private void saveGpu(Product product, ComponentSpecRequest req) {
        requireIntMin(req.getVramGb(), 0, "VRAM");
        requireIntMin(req.getLengthMm(), 0, "Chiều dài GPU");
        requireIntMin(req.getRecommendedPsuWatt(), 0, "PSU khuyến nghị");
        requireIntMin(req.getSlotWidth(), 1, "Độ dày (số khe slot)");
        requireLength(req.getPowerConnector(), 50, "Đầu cấp nguồn");

        Gpu gpu = new Gpu();
        gpu.setProduct(product);
        gpu.setVramGb(req.getVramGb());
        gpu.setLengthMm(req.getLengthMm());
        gpu.setPowerConnector(req.getPowerConnector());
        gpu.setRecommendedPsuWatt(req.getRecommendedPsuWatt());
        gpu.setSlotWidth(req.getSlotWidth() != null ? req.getSlotWidth() : 2);
        gpuRepository.save(gpu);
    }

    private void savePsu(Product product, ComponentSpecRequest req) {
        if (req.getWattage() == null) {
            throw new IllegalArgumentException("Vui lòng nhập công suất (Wattage) cho PSU.");
        }
        requireIntMin(req.getWattage(), 1, "Công suất (Wattage)");
        requireLength(req.getEfficiencyRating(), 30, "Chuẩn hiệu suất");
        requireLength(req.getPsuFormFactor(), 20, "Form Factor PSU");

        Psu.Modular modular = parseOptionalEnum(Psu.Modular.class, req.getModular(), Psu.Modular.FULL, "Modular");

        Psu psu = new Psu();
        psu.setProduct(product);
        psu.setWattage(req.getWattage());
        psu.setEfficiencyRating(req.getEfficiencyRating());
        psu.setModular(modular);
        psu.setFormFactor(req.getPsuFormFactor() != null && !req.getPsuFormFactor().isBlank()
                ? req.getPsuFormFactor().trim() : "ATX");
        psuRepository.save(psu);
    }

    private void saveCase(Product product, ComponentSpecRequest req) {
        if (req.getCaseFormFactorSupport() == null || req.getCaseFormFactorSupport().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 Form Factor mà Case hỗ trợ.");
        }
        requireIntMin(req.getMaxGpuLengthMm(), 0, "GPU dài tối đa");
        requireIntMin(req.getMaxCoolerHeightMm(), 0, "Tản khí cao tối đa");
        requireIntMin(req.getMaxRadiatorMm(), 0, "Radiator tối đa");

        CaseComponent c = new CaseComponent();
        c.setProduct(product);
        c.setFormFactorSupport(String.join(",", req.getCaseFormFactorSupport()));
        c.setMaxGpuLengthMm(req.getMaxGpuLengthMm());
        c.setMaxCoolerHeightMm(req.getMaxCoolerHeightMm());
        c.setMaxRadiatorMm(req.getMaxRadiatorMm());
        caseRepository.save(c);
    }

    private void saveCooler(Product product, ComponentSpecRequest req) {
        requireIntMin(req.getHeightMm(), 0, "Chiều cao tản khí");
        requireIntMin(req.getRadiatorSizeMm(), 0, "Kích thước Radiator");
        requireLength(req.getSocketSupport(), 200, "Socket hỗ trợ");

        Cooler.CoolerType coolerType = parseRequiredEnum(Cooler.CoolerType.class, req.getCoolerType(), "Loại tản nhiệt (Khí/AIO)");

        Cooler cooler = new Cooler();
        cooler.setProduct(product);
        cooler.setCoolerType(coolerType);
        cooler.setHeightMm(req.getHeightMm());
        cooler.setRadiatorSizeMm(req.getRadiatorSizeMm());
        cooler.setSocketSupport(req.getSocketSupport());
        coolerRepository.save(cooler);
    }

    private void saveStorage(Product product, ComponentSpecRequest req) {
        if (req.getStorageCapacityGb() == null) {
            throw new IllegalArgumentException("Vui lòng nhập Dung lượng cho ổ cứng.");
        }
        requireIntMin(req.getStorageCapacityGb(), 1, "Dung lượng ổ cứng");
        requireLength(req.getStorageInterface(), 30, "Chuẩn giao tiếp");

        Storage.StorageType storageType = parseRequiredEnum(Storage.StorageType.class, req.getStorageType(), "Loại ổ cứng");

        Storage storage = new Storage();
        storage.setProduct(product);
        storage.setStorageType(storageType);
        storage.setStorageInterface(req.getStorageInterface());
        storage.setCapacityGb(req.getStorageCapacityGb());
        storageRepository.save(storage);
    }

    @Transactional
    public void toggleStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

        product.setStatus(product.getStatus() == Product.ProductStatus.ACTIVE
                ? Product.ProductStatus.HIDDEN
                : Product.ProductStatus.ACTIVE);
        productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

        // // Kiểm tra có trong order không
        // long orderDetailCount = orderDetailRepository.countByProduct_ProductId(id);
        // if (orderDetailCount > 0) {
        //     throw new IllegalArgumentException(
        //             "Không thể xóa sản phẩm đang có " + orderDetailCount + " đơn hàng.");
        // }

        // Xóa images trước (cascade)
        productImageRepository.deleteByProduct_ProductId(id);
        productSpecificationRepository.deleteAllByProduct_ProductId(id);

        productRepository.delete(product);
    }

    private void validateName(String name, Long currentId) {
        boolean duplicate = (currentId == null)
                ? productRepository.existsByNameIgnoreCase(name)
                : productRepository.existsByNameIgnoreCaseAndProductIdNot(name, currentId);
        if (duplicate) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại.");
        }
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Vui lòng chọn danh mục.");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại."));
    }
}