package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gpus")
@Getter
@Setter
@NoArgsConstructor
public class Gpu {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "vram_gb")
    private Integer vramGb;

    @Column(name = "length_mm")
    private Integer lengthMm;

    @Column(name = "power_connector", length = 50)
    private String powerConnector;

    @Column(name = "recommended_psu_watt")
    private Integer recommendedPsuWatt;

    @Column(name = "slot_width")
    private Integer slotWidth = 2;
}