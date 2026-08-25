package com.swp391.techforge.controller.customer;

import com.swp391.techforge.dto.BuildPcProductDto;
import com.swp391.techforge.dto.BuildPcValidateRequest;
import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.service.PcBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buildpc")
@RequiredArgsConstructor
public class PcBuilderController {

    private final PcBuilderService pcBuilderService;

    @PostMapping("/validate")
    public ResponseEntity<CompatibilityReport> validateBuild(@RequestBody BuildPcValidateRequest request) {
        CompatibilityReport report = pcBuilderService.checkCompatibility(request);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/components")
    public ResponseEntity<List<BuildPcProductDto>> getComponents(@RequestParam String categoryName,
                                                                 @RequestParam(defaultValue = "basePrice,asc") String sort,
                                                                 @RequestParam(defaultValue = "50") int size,
                                                                 @ModelAttribute BuildPcValidateRequest selectedComponents) {
        return ResponseEntity.ok(pcBuilderService.getComponentsByCategory(categoryName, sort, size, selectedComponents));
    }
}
