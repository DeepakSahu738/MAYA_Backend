package com.MAYA.MAYA.DTO.snapchat;

import java.util.List;

public class SnapchatPostDTO {

    private String snapGoal; // Engagement, Awareness, Promotion, Storytelling
    private String niche; // Fitness, Fashion, Events, Gaming, etc.
    private String storyType; // Short Video, Snap Streaks, Behind-the-Scenes, Flashback
    private String targetAudience; // Age group, Interests, Preferred content style
    private String toneAndStyle; // Playful, Mysterious, Fast-Paced, Personal
    private String contentType; // Trending (Viral challenges) or Evergreen (Long-lasting series)
    private List<String> stickersAndFilters; // Custom branding or trending AR filters
    private String callToAction; // Swipe Up, DM, Share, Try This Filter

    // Constructor
    public SnapchatPostDTO() {
    }

    public SnapchatPostDTO(String snapGoal, String niche, String storyType, String targetAudience,
                           String toneAndStyle, String contentType, List<String> stickersAndFilters,
                           String callToAction) {
        this.snapGoal = snapGoal;
        this.niche = niche;
        this.storyType = storyType;
        this.targetAudience = targetAudience;
        this.toneAndStyle = toneAndStyle;
        this.contentType = contentType;
        this.stickersAndFilters = stickersAndFilters;
        this.callToAction = callToAction;
    }

    // Getters and Setters
    public String getSnapGoal() {
        return snapGoal;
    }

    public void setSnapGoal(String snapGoal) {
        this.snapGoal = snapGoal;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getStoryType() {
        return storyType;
    }

    public void setStoryType(String storyType) {
        this.storyType = storyType;
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

    public List<String> getStickersAndFilters() {
        return stickersAndFilters;
    }

    public void setStickersAndFilters(List<String> stickersAndFilters) {
        this.stickersAndFilters = stickersAndFilters;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    @Override
    public String toString() {
        return "SnapchatPostDTO{" +
                "snapGoal='" + snapGoal + '\'' +
                ", niche='" + niche + '\'' +
                ", storyType='" + storyType + '\'' +
                ", targetAudience='" + targetAudience + '\'' +
                ", toneAndStyle='" + toneAndStyle + '\'' +
                ", contentType='" + contentType + '\'' +
                ", stickersAndFilters=" + stickersAndFilters +
                ", callToAction='" + callToAction + '\'' +
                '}';
    }
}
