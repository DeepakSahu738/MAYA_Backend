package com.MAYA.MAYA.DTO;

public class instaPrompt_TWO {

    private String contentIdea;
    private String toneStyle;
    private String callToAction;

    // Getters & Setters
    public String getContentIdea() {
        return contentIdea;
    }

    public void setContentIdea(String contentIdea) {
        this.contentIdea = contentIdea;
    }

    public String getToneStyle() {
        return toneStyle;
    }

    public void setToneStyle(String toneStyle) {
        this.toneStyle = toneStyle;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
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