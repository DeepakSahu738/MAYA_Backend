package com.MAYA.MAYA.DTO.pinterest;

import java.util.List;

public class PinterestPostDTO {


    private String pinGoal; // Traffic, Engagement, Brand Awareness, Affiliate Sales
    private String niche; // DIY, Home Decor, Fitness, Recipes, etc.
    private String pinType; // Single Image, Infographic, Story Pin, Video Pin
    private String targetAudience; // Demographics, Interests, Search behavior
    private String toneAndStyle; // Aesthetic, Minimalist, Step-by-Step, Lifestyle
    private String contentType; // Trending (Viral ideas) or Evergreen (Long-lasting inspiration)
    private List<String> searchKeywords; // SEO-friendly Pinterest terms
    private String callToAction; // Click to Read, Save, Visit Website

    // Constructor
    public PinterestPostDTO() {
    }

    public PinterestPostDTO(String pinGoal, String niche, String pinType, String targetAudience,
                            String toneAndStyle, String contentType, List<String> searchKeywords,
                            String callToAction) {
        this.pinGoal = pinGoal;
        this.niche = niche;
        this.pinType = pinType;
        this.targetAudience = targetAudience;
        this.toneAndStyle = toneAndStyle;
        this.contentType = contentType;
        this.searchKeywords = searchKeywords;
        this.callToAction = callToAction;
    }

    // Getters and Setters
    public String getPinGoal() {
        return pinGoal;
    }

    public void setPinGoal(String pinGoal) {
        this.pinGoal = pinGoal;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getPinType() {
        return pinType;
    }

    public void setPinType(String pinType) {
        this.pinType = pinType;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getToneAndStyle() {
        return toneAndStyle;
    }

    public void setToneAndStyle(String toneAndStyle) {
        this.toneAndStyle = toneAndStyle;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<String> getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(List<String> searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    @Override
    public String toString() {
        return "PinterestPostDTO{" +
                "pinGoal='" + pinGoal + '\'' +
                ", niche='" + niche + '\'' +
                ", pinType='" + pinType + '\'' +
                ", targetAudience='" + targetAudience + '\'' +
                ", toneAndStyle='" + toneAndStyle + '\'' +
                ", contentType='" + contentType + '\'' +
                ", searchKeywords=" + searchKeywords +
                ", callToAction='" + callToAction + '\'' +
                '}';
    }
}
