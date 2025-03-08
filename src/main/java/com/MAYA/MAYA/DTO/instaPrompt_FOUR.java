package com.MAYA.MAYA.DTO;

public class instaPrompt_FOUR {

    private String niche;
    private String toneStyle;
    private String contentType;

    // Getters & Setters
    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getToneStyle() {
        return toneStyle;
    }

    public void setToneStyle(String toneStyle) {
        this.toneStyle = toneStyle;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
// json input for Postman -
/*{
  "niche": "Personal Finance",
  "toneStyle": "Urgent & Eye-catching",
  "contentType": "Reel"
}*/

