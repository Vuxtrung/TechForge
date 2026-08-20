package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coolers")
@Getter
@Setter
@NoArgsConstructor
public class Cooler {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull(message = "Loại tản nhiệt không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "cooler_type", nullable = false, length = 10)
    private CoolerType coolerType;

    @Column(name = "height_mm")
    private Integer heightMm;

    @Column(name = "radiator_size_mm")
    private Integer radiatorSizeMm;

    @Column(name = "socket_support", length = 200)
    private String socketSupport;

    public enum CoolerType {
        AIR, AIO
    }
}