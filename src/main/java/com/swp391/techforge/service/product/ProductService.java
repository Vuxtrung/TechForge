package com.swp391.techforge.service.product;

import com.swp391.techforge.entity.Category;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.repository.category.CategoryRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
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
    public Product create(Product product) {
        validateName(product.getName(), null);
        product.setCategory(resolveCategory(product.getCategoryId()));
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product incoming) {
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

        return productRepository.save(existing);
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