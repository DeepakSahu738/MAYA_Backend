package com.MAYA.MAYA.DTO.instagram;



import java.util.List;

public class CombinedFieldsForChainFlowDTO {

    private String contentGoal;
    private String toneStyle;
    private String callToAction;
    private String niche;
    private String targetAudience;
    private List<String> keywords;
    private String trendingOrEvergreen;
    private String contentType;

    public String getContentGoal() {
        return contentGoal;
    }

    public String getToneStyle() {
        return toneStyle;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public String getNiche() {
        return niche;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getTrendingOrEvergreen() {
        return trendingOrEvergreen;
    }

    public String getContentType() {
        return contentType;
    }

    // Setters

    public void setContentGoal(String contentGoal) {
        this.contentGoal = contentGoal;
    }

    public void setToneStyle(String toneStyle) {
        this.toneStyle = toneStyle;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    public void setNiche(String contentIdea) {
        this.niche = contentIdea;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void setTrendingOrEvergreen(String trendingOrEvergreen) {
        this.trendingOrEvergreen = trendingOrEvergreen;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


}
