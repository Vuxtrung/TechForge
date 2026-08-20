package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "cpus")
@Getter
@Setter
@NoArgsConstructor
public class Cpu {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @NotBlank(message = "Socket không được để trống")
    @Column(name = "socket", nullable = false, length = 30)
    private String socket;

    @Column(name = "cores")
    private Integer cores;

    @Column(name = "threads")
    private Integer threads;

    @Column(name = "base_clock_ghz", precision = 4, scale = 2)
    private BigDecimal baseClockGhz;

    @Column(name = "boost_clock_ghz", precision = 4, scale = 2)
    private BigDecimal boostClockGhz;

    @Column(name = "tdp_watt")
    private Integer tdpWatt;

    @Column(name = "has_igpu")
    private Boolean hasIgpu = false;
}