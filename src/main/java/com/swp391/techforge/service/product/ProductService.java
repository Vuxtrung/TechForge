package com.swp391.techforge.service.product;

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

import java.io.IOException;
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

    public ProductService(ProductRepository productRepository,
                           CategoryRepository categoryRepository,
                           ProductImageRepository productImageRepository,
                           CloudinaryService cloudinaryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
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

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        product.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null);
        return product;
    }

    @Transactional
    public Product create(Product product, MultipartFile imageFile) {
        validateName(product.getName(), null);
        product.setCategory(resolveCategory(product.getCategoryId()));
        Product saved = productRepository.save(product);

        if (imageFile != null && !imageFile.isEmpty()) {
            uploadAndAttachImage(saved, imageFile, true);
        }
        return saved;
    }

    @Transactional
    public Product update(Long id, Product incoming, MultipartFile imageFile) {
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

        if (imageFile != null && !imageFile.isEmpty()) {
            boolean hasPrimary = existing.getImages().stream()
                    .anyMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));
            uploadAndAttachImage(existing, imageFile, !hasPrimary);
        }

        return productRepository.save(existing);
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