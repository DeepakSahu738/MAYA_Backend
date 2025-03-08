package com.MAYA.MAYA.DTO.instagram;

import java.util.List;

public class HashtagsDTO {
    private String niche;
    private List<String> keywords;
    private String trendingOrEvergreen;

    // Getters & Setters
    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getTrendingOrEvergreen() {
        return trendingOrEvergreen;
    }

    public void setTrendingOrEvergreen(String trendingOrEvergreen) {
        this.trendingOrEvergreen = trendingOrEvergreen;
    }

}

// json input for Postman -
/* {
  "niche": "Fitness",
  "keywords": ["Workout", "Healthy Living"],
  "trendingOrEvergreen": "Trending"
}*/

//dummy response
/*{
  "hashtags": ["#workout", "#healthyliving", "#fitness", "#trending"]
}*/