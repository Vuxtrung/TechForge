package com.swp391.techforge.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor

public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 255, message = "Mô tả danh mục không được vượt quá 255 ký tự")
    @Column(name = "description", length = 255)
    private String description;

    @NotNull(message = "Vui long chọn loại danh mục")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CategoryType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Transient
    private Long parentId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private Long productCount = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CategoryType {
        PC_PRODUCT,
        PC_COMPONENT
    }

}
