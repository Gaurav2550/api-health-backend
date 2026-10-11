package com.apihealth.optimization.dto;

import java.util.List;

public class RecommendationResponse {

    private String category;
    private String title;
    private String description;
    private String impact; // HIGH, MEDIUM, LOW
    private List<String> actionableSteps;

    public RecommendationResponse() {}

    public RecommendationResponse(String category, String title, String description, String impact, List<String> actionableSteps) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.impact = impact;
        this.actionableSteps = actionableSteps;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public List<String> getActionableSteps() {
        return actionableSteps;
    }

    public void setActionableSteps(List<String> actionableSteps) {
        this.actionableSteps = actionableSteps;
    }
}
