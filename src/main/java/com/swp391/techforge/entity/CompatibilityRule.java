package com.swp391.techforge.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "compatibility_rules")
public class CompatibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ruleName;

    // Biểu thức dạng SpEL, ví dụ: '#CPU_Socket == #Mainboard_Socket'
    private String expression;

    // Nội dung thông báo
    private String message;

    // Loại thông báo: ERROR, WARNING
    private String severity;

    private Boolean isActive = true;

    // Constructors
    public CompatibilityRule() {
    }

    public CompatibilityRule(String ruleName, String expression, String message, String severity, Boolean isActive) {
        this.ruleName = ruleName;
        this.expression = expression;
        this.message = message;
        this.severity = severity;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
