package com.MAYA.MAYA.DTO.tiktok;

import java.util.List;

public class TikTokPostDTO {

    private String videoGoal; // Engagement, Growth, Brand Visibility, Challenge Participation
    private String niche; // Dance, Comedy, Tech, Lifestyle, Business, etc.
    private String videoFormat; // Trending Dance, POV, Storytelling, Tutorial, Duet
    private String targetAudience; // Age, Interests, Preferred content style
    private String toneAndStyle; // Fun, Informative, Aesthetic, Controversial
    private String contentType; // Trending (Viral challenges) or Evergreen (Timeless content)
    private List<String> soundsAndEffects; // Trending audios or original sounds
    private String callToAction; // Follow, Stitch, Duet, Click Link in Bio

    // Constructor
    public TikTokPostDTO() {
    }

    public TikTokPostDTO(String videoGoal, String niche, String videoFormat, String targetAudience,
                         String toneAndStyle, String contentType, List<String> soundsAndEffects,
                         String callToAction) {
        this.videoGoal = videoGoal;
        this.niche = niche;
        this.videoFormat = videoFormat;
        this.targetAudience = targetAudience;
        this.toneAndStyle = toneAndStyle;
        this.contentType = contentType;
        this.soundsAndEffects = soundsAndEffects;
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

    public String getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(String videoFormat) {
        this.videoFormat = videoFormat;
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

    public List<String> getSoundsAndEffects() {
        return soundsAndEffects;
    }

    public void setSoundsAndEffects(List<String> soundsAndEffects) {
        this.soundsAndEffects = soundsAndEffects;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    @Override
    public String toString() {
        return "TikTokPostDTO{" +
                "videoGoal='" + videoGoal + '\'' +
                ", niche='" + niche + '\'' +
                ", videoFormat='" + videoFormat + '\'' +
                ", targetAudience='" + targetAudience + '\'' +
                ", toneAndStyle='" + toneAndStyle + '\'' +
                ", contentType='" + contentType + '\'' +
                ", soundsAndEffects=" + soundsAndEffects +
                ", callToAction='" + callToAction + '\'' +
                '}';
    }
}
