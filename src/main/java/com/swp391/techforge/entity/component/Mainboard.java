package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mainboards")
@Getter
@Setter
@NoArgsConstructor
public class Mainboard {

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

    @Column(name = "chipset", length = 30)
    private String chipset;

    @NotNull(message = "Loại RAM không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "ram_type", nullable = false, length = 10)
    private RamType ramType;

    @Column(name = "ram_slots")
    private Integer ramSlots;

    @Column(name = "max_ram_gb")
    private Integer maxRamGb;

    @NotNull(message = "Form factor không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "form_factor", nullable = false, length = 20)
    private FormFactor formFactor;

    @Column(name = "m2_slots")
    private Integer m2Slots;

    public enum RamType {
        DDR4, DDR5
    }

    public enum FormFactor {
        ATX, MICRO_ATX, MINI_ITX, E_ATX
    }
}