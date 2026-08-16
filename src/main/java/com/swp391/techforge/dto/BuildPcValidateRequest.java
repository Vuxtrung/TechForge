package com.swp391.techforge.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuildPcValidateRequest {
    private Long cpuId;
    private Long mainboardId;
    private Long ramId;
    private Long vgaId;
    private Long psuId;
    private Long caseId;
    private Long storageId;
    private Long coolerId;
}
