package com.swp391.techforge.service.category;

import java.time.LocalDateTime;
import java.util.Comparator;

import com.swp391.techforge.entity.Category;
import com.swp391.techforge.repository.category.CategoryRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Lấy N danh mục nổi bật (nhiều sản phẩm nhất), dùng cho trang chủ
    @Transactional(readOnly = true)
    public List<Category> findTopCategoriesByProductCount(int limit) {
        List<Long> topIds = categoryRepository.findTopRootCategoryIdsByProductCount(limit);
        List<Category> categories = categoryRepository.findAllById(topIds);

        // findAllById không đảm bảo giữ thứ tự -> sắp lại theo đúng thứ tự topIds (đã ORDER BY count DESC)
        categories.sort(Comparator.comparingInt(c -> topIds.indexOf(c.getCategoryId())));
        return categories;
    }

    @Transactional(readOnly = true)
    public Page<Category> search(String keyword, String type, String active,
            int page, int size, Sort sort) {
        Boolean activeBool = (active == null || active.isBlank()) ? null : Boolean.valueOf(active);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Category> result = categoryRepository.search(keyword, type, activeBool, pageable);
        result.forEach(c -> {
            c.setParentId(c.getParent() != null ? c.getParent().getCategoryId() : null);
            // Đổi sang đếm đệ quy: gồm cả sản phẩm của danh mục con
            c.setProductCount(categoryRepository.countProductsByCategoryIdRecursive(c.getCategoryId()));
        });
        return result;
    }

    // Chọn cha trong form
    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> findAllChild() {
        return categoryRepository.findAllByParentIsNotNullAndActiveTrueOrderByNameAsc();
    }

    // Chỉ lấy danh mục GỐC (không có cha) đang active - dùng cho dropdown "Danh Mục Cha"
    // để giới hạn cây chỉ tối đa 2 cấp (gốc -> con), không cho chọn con làm cha tiếp
    @Transactional(readOnly = true)
    public List<Category> findRootActive() {
        return categoryRepository.findAllByParentIsNullAndActiveTrueOrderByNameAsc();
    }

    // Lấy 1 category theo ID - Dùng cho form sửa
    @Transactional(readOnly = true)
    public Category getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));
        category.setParentId(category.getParent() != null ? category.getParent().getCategoryId() : null);
        return category;
    }

    @Transactional
    public Category create(Category category) {
        validateName(category.getName(), null);
        category.setParent(resolveParent(category.getParentId(), null));
        normalizeComponentType(category);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, Category incoming) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));

        validateName(incoming.getName(), id);

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setType(incoming.getType());
        existing.setComponentType(incoming.getComponentType());
        existing.setActive(incoming.isActive());
        existing.setParent(resolveParent(incoming.getParentId(), id));
        existing.setUpdatedAt(LocalDateTime.now());
        normalizeComponentType(existing);

        return categoryRepository.save(existing);
    }

    @Transactional
    public void toggleActive(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));

        boolean newStatus = !category.isActive();
        category.setActive(newStatus);
        categoryRepository.save(category);

        // Cascade: nếu ẩn danh mục cha thì ẩn luôn toàn bộ danh mục con (đệ quy)
        if (!newStatus) {
            cascadeDeactivate(id);
        }
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));

        List<Category> children = categoryRepository.findAllByParent_CategoryId(id);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa danh mục đang có danh mục con bên trong.");
        }

        long productCount = categoryRepository.countProductsByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException("Không thể xóa danh mục đang có sản phẩm.");
        }

        categoryRepository.delete(category);
    }

    // Đệ quy ẩn danh mục con khi ẩn danh mục cha
    private void cascadeDeactivate(Long parentId) {
        List<Category> children = categoryRepository.findAllByParent_CategoryId(parentId);
        for (Category child : children) {
            if (child.isActive()) {
                child.setActive(false);
                categoryRepository.save(child);
            }
            cascadeDeactivate(child.getCategoryId());
        }
    }

    // PC dựng sẵn (PC_PRODUCT) không dùng bảng linh kiện (dùng product_specifications
    // như trước) -> luôn ép componentType = NONE để tránh admin chọn nhầm, khiến
    // Product form/Service hiểu sai và cố lưu vào bảng linh kiện không liên quan.
    private void normalizeComponentType(Category category) {
        if (category.getType() == Category.CategoryType.PC_PRODUCT) {
            category.setComponentType(Category.ComponentType.NONE);
        } else if (category.getComponentType() == null) {
            category.setComponentType(Category.ComponentType.NONE);
        }
    }

    private void validateName(String name, Long currentId) {
        boolean duplicate = (currentId == null)
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndCategoryIdNot(name, currentId);
        if (duplicate) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại.");
        }
    }

    private Category resolveParent(Long parentId, Long currentId) {
        if (parentId == null) {
            return null;
        }

        if (currentId != null && parentId.equals(currentId)) {
            throw new IllegalArgumentException(
                    "Danh mục không thể là cha của chính nó.");
        }

        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException(
                "Danh mục cha không tồn tại."));

        if (parent.getParent() != null) {
            throw new IllegalArgumentException(
                    "Chỉ được phép tạo danh mục tối đa 2 tầng.");
        }

        return parent;
    }
}
