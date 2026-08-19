package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rams")
@Getter
@Setter
@NoArgsConstructor
public class Ram {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull(message = "Loại RAM không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "ram_type", nullable = false, length = 10)
    private RamType ramType;

    @NotNull(message = "Tốc độ RAM không được để trống")
    @Column(name = "speed_mhz", nullable = false)
    private Integer speedMhz;

    @NotNull(message = "Dung lượng RAM không được để trống")
    @Column(name = "capacity_gb", nullable = false)
    private Integer capacityGb;

    @Column(name = "modules")
    private Integer modules = 1;

    public enum RamType {
        DDR4, DDR5
    }
}