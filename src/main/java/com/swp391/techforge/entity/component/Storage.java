package com.swp391.techforge.entity.component;

import com.swp391.techforge.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storages")
@Getter
@Setter
@NoArgsConstructor
public class Storage {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull(message = "Loại lưu trữ không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 10)
    private StorageType storageType;

    @Column(name = "interface", length = 30)
    private String storageInterface;

    @NotNull(message = "Dung lượng không được để trống")
    @Column(name = "capacity_gb", nullable = false)
    private Integer capacityGb;

    public enum StorageType {
        SSD_NVME, SSD_SATA, HDD
    }
}