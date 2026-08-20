package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "psus")
@Getter
@Setter
@NoArgsConstructor
public class Psu {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull(message = "Công suất nguồn không được để trống")
    @Column(name = "wattage", nullable = false)
    private Integer wattage;

    @Column(name = "efficiency_rating", length = 30)
    private String efficiencyRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "modular", length = 10)
    private Modular modular = Modular.FULL;

    @Column(name = "form_factor", length = 20)
    private String formFactor = "ATX";

    public enum Modular {
        FULL, SEMI, NONE
    }
}