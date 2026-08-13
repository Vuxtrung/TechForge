package com.swp391.techforge.service.category;

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

    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc();
    }

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
        existing.setActive(incoming.isActive());
        existing.setParent(resolveParent(incoming.getParentId(), id));

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
            throw new IllegalArgumentException("Danh mục không thể là cha của chính nó.");
        }

        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại."));

        // Chống vòng lặp cha-con đa cấp: duyệt ngược lên tổ tiên của parent,
        // nếu gặp lại currentId thì tức là đang tạo vòng lặp (A -> B -> A...)
        if (currentId != null) {
            Category ancestor = parent;
            while (ancestor != null) {
                if (ancestor.getCategoryId().equals(currentId)) {
                    throw new IllegalArgumentException(
                            "Không thể chọn danh mục con của chính nó làm danh mục cha.");
                }
                ancestor = ancestor.getParent();
            }
        }

        return parent;
    }
}