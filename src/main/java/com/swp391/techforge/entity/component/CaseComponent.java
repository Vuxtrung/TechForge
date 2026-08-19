package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
public class CaseComponent {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @NotBlank(message = "Form factor hỗ trợ không được để trống")
    @Column(name = "form_factor_support", nullable = false, length = 100)
    private String formFactorSupport;

    @Column(name = "max_gpu_length_mm")
    private Integer maxGpuLengthMm;

    @Column(name = "max_cooler_height_mm")
    private Integer maxCoolerHeightMm;

    @Column(name = "max_radiator_mm")
    private Integer maxRadiatorMm;
}