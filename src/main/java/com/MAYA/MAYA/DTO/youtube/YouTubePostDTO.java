package com.MAYA.MAYA.DTO.youtube;

import java.util.List;

public class YouTubePostDTO {

    private String videoGoal; // Engagement, Subscribers, Monetization, Brand Awareness
    private String niche; // Tech Reviews, Fitness, Vlogs, Tutorials, etc.
    private String videoType; // Shorts, Vlog, Podcast, How-To, Review, Documentary
    private String targetAudience; // Age, Interests, Watch habits
    private String toneAndStyle; // Educational, Storytelling, Entertaining, Professional
    private String contentType; // Trending (Challenges) or Evergreen (Timeless videos)
    private List<String> keywordsAndSeoTags; // Search-friendly words & topics
    private String callToAction; // Subscribe, Like, Comment, Share

    // Constructor
    public YouTubePostDTO() {
    }

    public YouTubePostDTO(String videoGoal, String niche, String videoType, String targetAudience,
                          String toneAndStyle, String contentType, List<String> keywordsAndSeoTags,
                          String callToAction) {
        this.videoGoal = videoGoal;
        this.niche = niche;
        this.videoType = videoType;
        this.targetAudience = targetAudience;
        this.toneAndStyle = toneAndStyle;
        this.contentType = contentType;
        this.keywordsAndSeoTags = keywordsAndSeoTags;
        this.callToAction = callToAction;
    }

    // Getters and Setters
    public String getVideoGoal() {
        return videoGoal;
    }

    public void setVideoGoal(String videoGoal) {
        this.videoGoal = videoGoal;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
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

    public List<String> getKeywordsAndSeoTags() {
        return keywordsAndSeoTags;
    }

    public void setKeywordsAndSeoTags(List<String> keywordsAndSeoTags) {
        this.keywordsAndSeoTags = keywordsAndSeoTags;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    @Override
    public String toString() {
        return "YouTubePostDTO{" +
                "videoGoal='" + videoGoal + '\'' +
                ", niche='" + niche + '\'' +
                ", videoType='" + videoType + '\'' +
                ", targetAudience='" + targetAudience + '\'' +
                ", toneAndStyle='" + toneAndStyle + '\'' +
                ", contentType='" + contentType + '\'' +
                ", keywordsAndSeoTags=" + keywordsAndSeoTags +
                ", callToAction='" + callToAction + '\'' +
                '}';
    }
}
