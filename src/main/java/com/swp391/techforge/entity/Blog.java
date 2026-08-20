package com.swp391.techforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Thực thể bài viết Blog & Tin tức công nghệ
 */
@Entity
@Table(name = "blogs", indexes = {
    @Index(name = "idx_blog_slug", columnList = "slug"),
    @Index(name = "idx_blog_status", columnList = "status"),
    @Index(name = "idx_blog_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BlogStatus status = BlogStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "views_count", nullable = false)
    private Integer viewsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.viewsCount == null) {
            this.viewsCount = 0;
        }
        if (this.status == BlogStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == BlogStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }
}
