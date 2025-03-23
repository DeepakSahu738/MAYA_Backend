package com.MAYA.MAYA.DTO.facebook;

import java.util.List;

public class FacebookPostDTO {
    private String postGoal; // Engagement, Awareness, Lead Generation, Traffic
    private String niche; // Business, News, Tech, Lifestyle, etc.
    private String postType; // Text Post, Image Post, Video, Story, Live, Link
    private String targetAudience; // Demographics, Interests, Groups they engage with
    private String toneAndStyle; // Informative, Emotional, Storytelling, Relatable
    private String contentType; // Trending (News-based) or Evergreen
    private List<String> topicsAndKeywords; // Specific subjects to cover
    private String callToAction; // Share, Comment, Visit Website, Join Group

    // Constructor
    public FacebookPostDTO() {
    }

    public FacebookPostDTO(String postGoal, String niche, String postType, String targetAudience,
                           String toneAndStyle, String contentType, List<String> topicsAndKeywords,
                           String callToAction) {
        this.postGoal = postGoal;
        this.niche = niche;
        this.postType = postType;
        this.targetAudience = targetAudience;
        this.toneAndStyle = toneAndStyle;
        this.contentType = contentType;
        this.topicsAndKeywords = topicsAndKeywords;
        this.callToAction = callToAction;
    }

    // Getters and Setters
    public String getPostGoal() {
        return postGoal;
    }

    public void setPostGoal(String postGoal) {
        this.postGoal = postGoal;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
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

    public List<String> getTopicsAndKeywords() {
        return topicsAndKeywords;
    }

    public void setTopicsAndKeywords(List<String> topicsAndKeywords) {
        this.topicsAndKeywords = topicsAndKeywords;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    @Override
    public String toString() {
        return "FacebookPostDTO{" +
                "postGoal='" + postGoal + '\'' +
                ", niche='" + niche + '\'' +
                ", postType='" + postType + '\'' +
                ", targetAudience='" + targetAudience + '\'' +
                ", toneAndStyle='" + toneAndStyle + '\'' +
                ", contentType='" + contentType + '\'' +
                ", topicsAndKeywords=" + topicsAndKeywords +
                ", callToAction='" + callToAction + '\'' +
                '}';
    }
}
