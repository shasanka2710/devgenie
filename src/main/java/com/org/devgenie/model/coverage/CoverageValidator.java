package com.org.devgenie.model.coverage;

import com.org.devgenie.config.CoverageConfiguration;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CoverageValidator {

    @Autowired
    private CoverageConfiguration config;

    public ValidationResult validateCoverageImprovement(CoverageData before, CoverageData after) {
        ValidationResult result = new ValidationResult();

        // Check if coverage actually improved
        if (after.getOverallCoverage() <= before.getOverallCoverage()) {
            result.addError("Overall coverage did not improve");
        }

        // Check if it meets quality thresholds
        if (after.getMethodCoverage() < config.getQualityThresholds().getMinimumMethodCoverage()) {
            result.addWarning("Method coverage below threshold: " +
                    config.getQualityThresholds().getMinimumMethodCoverage() + "%");
        }

        if (after.getLineCoverage() < config.getQualityThresholds().getMinimumLineCoverage()) {
            result.addWarning("Line coverage below threshold: " +
                    config.getQualityThresholds().getMinimumLineCoverage() + "%");
        }

        if (after.getBranchCoverage() < config.getQualityThresholds().getMinimumBranchCoverage()) {
            result.addWarning("Branch coverage below threshold: " +
                    config.getQualityThresholds().getMinimumBranchCoverage() + "%");
        }

        return result;
    }

    @Data
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
