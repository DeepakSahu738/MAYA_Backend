package com.MAYA.MAYA.DTO;

public class instaPrompt_ONE {

    private String contentGoal;
    private String niche;
    private String contentType;
    private String trendingOrEvergreen;
    private String targetAudience;

    // Getters and Setters
    public String getContentGoal() {
        return contentGoal;
    }

    public void setContentGoal(String contentGoal) {
        this.contentGoal = contentGoal;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getTrendingOrEvergreen() {
        return trendingOrEvergreen;
    }

    public void setTrendingOrEvergreen(String trendingOrEvergreen) {
        this.trendingOrEvergreen = trendingOrEvergreen;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }
}

// json input for Postman -
/* {
  "contentIdea": "5 Ways to Stay Productive",
  "toneStyle": "Motivational",
  "callToAction": "Tag a friend who needs this!"
}*/

//dummy response
/*{
  "caption": " 5 Ways to Stay Productive \nLet's talk about it in a Motivational way!\nTag a friend who needs this!"
}*/