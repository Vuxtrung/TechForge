package com.swp391.techforge.service.product;

import com.swp391.techforge.dto.product.ProductFilterOptions;
import com.swp391.techforge.dto.product.ProductFilterRequest;
import com.swp391.techforge.entity.Category;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.ProductImage;
import com.swp391.techforge.repository.category.CategoryRepository;
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

import java.io.IOException;
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

    public ProductService(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository productImageRepository,
            ProductSpecificationRepository productSpecificationRepository,
            CloudinaryService cloudinaryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.productSpecificationRepository = productSpecificationRepository;
        this.cloudinaryService = cloudinaryService;
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

    @Transactional
    public Product create(Product product, MultipartFile imageFile,
            List<String> specKeys, List<String> specValues) {
        validateName(product.getName(), null);
        product.setCategory(resolveCategory(product.getCategoryId()));
        Product saved = productRepository.save(product);

        if (imageFile != null && !imageFile.isEmpty()) {
            uploadAndAttachImage(saved, imageFile, true);
        }
        saveSpecifications(saved, specKeys, specValues);
        return saved;
    }

    @Transactional
    public Product update(Long id, Product incoming, MultipartFile imageFile,
            List<String> specKeys, List<String> specValues) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

        validateName(incoming.getName(), id);

        existing.setName(incoming.getName());
        existing.setBrand(incoming.getBrand());
        existing.setDescription(incoming.getDescription());
        existing.setBasePrice(incoming.getBasePrice());
        existing.setStockQuantity(incoming.getStockQuantity());
        existing.setStatus(incoming.getStatus());
        existing.setCategory(resolveCategory(incoming.getCategoryId()));
        existing.setUpdatedAt(LocalDateTime.now());

        if (imageFile != null && !imageFile.isEmpty()) {
            // Xóa tất cả ảnh cũ
            productImageRepository.deleteByProduct_ProductId(existing.getProductId());
            // Upload ảnh mới là primary
            uploadAndAttachImage(existing, imageFile, true);
        }
        Product saved = productRepository.save(existing);
        saveSpecifications(saved, specKeys, specValues);
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

    private void saveSpecifications(Product product, List<String> specKeys, List<String> specValues) {
        productSpecificationRepository.deleteAllByProduct_ProductId(product.getProductId());
        if (specKeys == null || specValues == null) {
            return;
        }
        for (int i = 0; i < specKeys.size(); i++) {
            String key = specKeys.get(i) == null ? "" : specKeys.get(i).trim();
            String value = specValues.get(i) == null ? "" : specValues.get(i).trim();
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            ProductSpecification spec = new ProductSpecification();
            spec.setProduct(product);
            spec.setSpecKey(key);
            spec.setSpecValue(value);
            productSpecificationRepository.save(spec);
        }
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
