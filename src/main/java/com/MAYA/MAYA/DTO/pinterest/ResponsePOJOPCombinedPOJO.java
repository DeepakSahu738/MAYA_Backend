package com.MAYA.MAYA.DTO.pinterest;

import com.MAYA.MAYA.DTO.pinterest.aestheticAndDesign.ResponsePOJOPAestheticAndDesignWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.engagement.ResponsePOJOPEngagementWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.hashtag.ResponsePOJOPHashtagWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.pinIdea.ResponsePOJOPPinIdea;
import com.MAYA.MAYA.DTO.pinterest.pinIdea.ResponsePOJOPPinIdeaWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.postingTime.ResponsePOJOPPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.pinterest.titleAndDescription.ResponsePOJOPTitleAndDescriptionWRAPPER;

public class ResponsePOJOPCombinedPOJO {
    private ResponsePOJOPPinIdeaWRAPPER pinIdeaList;
    private ResponsePOJOPTitleAndDescriptionWRAPPER titleAndDescriptionList;
    private ResponsePOJOPHashtagWRAPPER hashtagList;
    private ResponsePOJOPAestheticAndDesignWRAPPER aestheticAndDesignList;
    private ResponsePOJOPEngagementWRAPPER engagementList;
    private ResponsePOJOPPostingTimeWRAPPER postingTimeList;

    public ResponsePOJOPPinIdeaWRAPPER getPinIdeaList() {
        return pinIdeaList;
    }

    public void setPinIdeaList(ResponsePOJOPPinIdeaWRAPPER pinIdeaList) {
        this.pinIdeaList = pinIdeaList;
    }

    public ResponsePOJOPTitleAndDescriptionWRAPPER getTitleAndDescriptionList() {
        return titleAndDescriptionList;
    }

    public void setTitleAndDescriptionList(ResponsePOJOPTitleAndDescriptionWRAPPER titleAndDescriptionList) {
        this.titleAndDescriptionList = titleAndDescriptionList;
    }

    public ResponsePOJOPHashtagWRAPPER getHashtagList() {
        return hashtagList;
    }

    public void setHashtagList(ResponsePOJOPHashtagWRAPPER hashtagList) {
        this.hashtagList = hashtagList;
    }

    public ResponsePOJOPAestheticAndDesignWRAPPER getAestheticAndDesignList() {
        return aestheticAndDesignList;
    }

    public void setAestheticAndDesignList(ResponsePOJOPAestheticAndDesignWRAPPER aestheticAndDesignList) {
        this.aestheticAndDesignList = aestheticAndDesignList;
    }

    public ResponsePOJOPEngagementWRAPPER getEngagementList() {
        return engagementList;
    }

    public void setEngagementList(ResponsePOJOPEngagementWRAPPER engagementList) {
        this.engagementList = engagementList;
    }

    public ResponsePOJOPPostingTimeWRAPPER getPostingTimeList() {
        return postingTimeList;
    }

    public void setPostingTimeList(ResponsePOJOPPostingTimeWRAPPER postingTimeList) {
        this.postingTimeList = postingTimeList;
    }
}

