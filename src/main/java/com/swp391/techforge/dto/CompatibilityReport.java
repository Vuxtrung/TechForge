package com.swp391.techforge.dto;

import java.util.ArrayList;
import java.util.List;

public class CompatibilityReport {
    private boolean isCompatible;
    private List<String> errors;
    private List<String> warnings;
    private int estimatedWattage;

    public CompatibilityReport() {
        this.isCompatible = true;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.estimatedWattage = 0;
    }

    public boolean isCompatible() {
        return isCompatible;
    }

    public void setCompatible(boolean compatible) {
        isCompatible = compatible;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public int getEstimatedWattage() {
        return estimatedWattage;
    }

    public void setEstimatedWattage(int estimatedWattage) {
        this.estimatedWattage = estimatedWattage;
    }
    
    public void addError(String error) {
        this.errors.add(error);
        this.isCompatible = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
