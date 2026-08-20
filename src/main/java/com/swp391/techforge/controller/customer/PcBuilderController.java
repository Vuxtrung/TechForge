package com.swp391.techforge.controller.customer;

import com.swp391.techforge.dto.BuildPcValidateRequest;
import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.service.PcBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pc-builder")
@RequiredArgsConstructor
public class PcBuilderController {

    private final PcBuilderService pcBuilderService;

    @PostMapping("/validate")
    public ResponseEntity<CompatibilityReport> validateBuild(@RequestBody BuildPcValidateRequest request) {
        CompatibilityReport report = pcBuilderService.checkCompatibility(request);
        return ResponseEntity.ok(report);
    }
}
