package com.MAYA.MAYA.DTO.twitterX;

import java.util.List;

public class TwitterPostDTO {

    private String tweetGoal; // Engagement, Growth, Brand Positioning, Virality
    private String niche; // Tech, Business, Personal Branding, Memes, etc.
    private String tweetType; // Text Tweet, Thread, Poll, Meme, Quote Tweet
    private String targetAudience; // Interests, Age, Tweeting habits
    private String toneAndStyle; // Witty, Informative, Thought-Provoking, Controversial
    private String contentType; // Trending (Viral topics) or Evergreen (Timeless insights)
    private List<String> hashtagsAndMentions; // Popular hashtags & accounts to tag
    private String callToAction; // Retweet, Follow, Reply, Click Link

    // Constructor
    public TwitterPostDTO() {
    }

    public TwitterPostDTO(String tweetGoal, String niche, String tweetType, String targetAudience,
                          String toneAndStyle, String contentType, List<String> hashtagsAndMentions,
                          String callToAction) {
        this.tweetGoal = tweetGoal;
        this.niche = niche;
        this.tweetType = tweetType;
        this.targetAudience = targetAudience;
        this.toneAndStyle = toneAndStyle;
        this.contentType = contentType;
        this.hashtagsAndMentions = hashtagsAndMentions;
        this.callToAction = callToAction;
    }

    // Getters and Setters
    public String getTweetGoal() {
        return tweetGoal;
    }

    public void setTweetGoal(String tweetGoal) {
        this.tweetGoal = tweetGoal;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getTweetType() {
        return tweetType;
    }

    public void setTweetType(String tweetType) {
        this.tweetType = tweetType;
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

    public List<String> getHashtagsAndMentions() {
        return hashtagsAndMentions;
    }

    public void setHashtagsAndMentions(List<String> hashtagsAndMentions) {
        this.hashtagsAndMentions = hashtagsAndMentions;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    @Override
    public String toString() {
        return "TwitterPostDTO{" +
                "tweetGoal='" + tweetGoal + '\'' +
                ", niche='" + niche + '\'' +
                ", tweetType='" + tweetType + '\'' +
                ", targetAudience='" + targetAudience + '\'' +
                ", toneAndStyle='" + toneAndStyle + '\'' +
                ", contentType='" + contentType + '\'' +
                ", hashtagsAndMentions=" + hashtagsAndMentions +
                ", callToAction='" + callToAction + '\'' +
                '}';
    }
}
