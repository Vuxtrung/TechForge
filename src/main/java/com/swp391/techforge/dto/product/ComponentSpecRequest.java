package com.swp391.techforge.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO nhận dữ liệu từ form admin product-form.html, dùng chung cho cả 2
 * nhánh: PC_PRODUCT (8 field cố định pcCpu..pcCase) và PC_COMPONENT (field
 * riêng theo từng loại linh kiện, chỉ nhóm field tương ứng componentType
 * được form hiển thị và gửi lên, các field còn lại sẽ null).
 */
@Getter
@Setter
@NoArgsConstructor
public class ComponentSpecRequest {

    // ==== PC_PRODUCT: 8 ô cố định (spec_key/spec_value) ====
    private String pcCpu;
    private String pcMainboard;
    private String pcRam;
    private String pcVga;
    private String pcStorage;
    private String pcPsu;
    private String pcCooler;
    private String pcCase;

    // ==== CPU ====
    // socket dùng chung cho cả CPU và Mainboard
    private String socket;
    private Integer cores;
    private Integer threads;
    private BigDecimal baseClockGhz;
    private BigDecimal boostClockGhz;
    private Integer tdpWatt;
    private Boolean hasIgpu;

    // ==== MAINBOARD ====
    private String chipset;
    private String mbRamType; // "DDR4" | "DDR5"
    private Integer ramSlots;
    private Integer maxRamGb;
    private String mbFormFactor; // "ATX" | "MICRO_ATX" | "MINI_ITX" | "E_ATX"
    private Integer m2Slots;

    // ==== RAM ====
    private String ramType; // "DDR4" | "DDR5"
    private Integer speedMhz;
    private Integer ramCapacityGb;
    private Integer modules;

    // ==== GPU ====
    private Integer vramGb;
    private Integer lengthMm;
    private String powerConnector;
    private Integer recommendedPsuWatt;
    private Integer slotWidth;

    // ==== PSU ====
    private Integer wattage;
    private String efficiencyRating;
    private String modular; // "FULL" | "SEMI" | "NONE"
    private String psuFormFactor;

    // ==== CASE ====
    private List<String> caseFormFactorSupport; // vd ["ATX","MICRO_ATX","MINI_ITX"]
    private Integer maxGpuLengthMm;
    private Integer maxCoolerHeightMm;
    private Integer maxRadiatorMm;

    // ==== COOLER ====
    private String coolerType; // "AIR" | "AIO"
    private Integer heightMm;
    private Integer radiatorSizeMm;
    private String socketSupport;

    // ==== STORAGE ====
    private String storageType; // "SSD_NVME" | "SSD_SATA" | "HDD"
    private String storageInterface;
    private Integer storageCapacityGb;
}
