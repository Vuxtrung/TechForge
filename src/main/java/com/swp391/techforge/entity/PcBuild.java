package com.swp391.techforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pc_builds")
@Getter
@Setter
@NoArgsConstructor
public class PcBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "build_id")
    private Long buildId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "build_name", length = 150)
    private String buildName;

    @Column(name = "budget_range", length = 50)
    private String budgetRange;

    @Column(name = "purpose", length = 100)
    private String purpose;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PcBuildStatus status = PcBuildStatus.DRAFT;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "pcBuild", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PcBuildItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
