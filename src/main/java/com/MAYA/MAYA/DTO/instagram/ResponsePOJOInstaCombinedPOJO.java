package com.MAYA.MAYA.DTO.instagram;

import com.MAYA.MAYA.DTO.instagram.captionPojo.ResponsePOJOInstaCaptionWRAPPER;
import com.MAYA.MAYA.DTO.instagram.contentIdeaPojo.ResponsePOJOInstaContentIdeaWRAPPER;
import com.MAYA.MAYA.DTO.instagram.designAndAestheticPojo.ResponsePOJOInstaDesignAndAestheticWRAPPER;
import com.MAYA.MAYA.DTO.instagram.engagementPojo.ResponsePOJOInstaEngagementStrategiesWRAPPER;
import com.MAYA.MAYA.DTO.instagram.hashtagsPojo.ResponsePOJOInstaHashtagsWRAPPER;
import com.MAYA.MAYA.DTO.instagram.postingTimePojo.ResponsePOJOInstaBestPostingTimeWRAPPER;

public class ResponsePOJOInstaCombinedPOJO {
    private ResponsePOJOInstaContentIdeaWRAPPER contentIdeaList;
    private ResponsePOJOInstaCaptionWRAPPER captionList;
    private ResponsePOJOInstaHashtagsWRAPPER hashtagsList;
    private ResponsePOJOInstaDesignAndAestheticWRAPPER designAndAestheticList;
    private ResponsePOJOInstaEngagementStrategiesWRAPPER engagementStrategiesList;
    private ResponsePOJOInstaBestPostingTimeWRAPPER postingTimeList;

    public ResponsePOJOInstaContentIdeaWRAPPER getContentIdeaList() {
        return contentIdeaList;
    }

    public void setContentIdeaList(ResponsePOJOInstaContentIdeaWRAPPER contentIdeaList) {
        this.contentIdeaList = contentIdeaList;
    }

    public ResponsePOJOInstaCaptionWRAPPER getCaptionList() {
        return captionList;
    }

    public void setCaptionList(ResponsePOJOInstaCaptionWRAPPER captionList) {
        this.captionList = captionList;
    }

    public ResponsePOJOInstaHashtagsWRAPPER getHashtagsList() {
        return hashtagsList;
    }

    public void setHashtagsList(ResponsePOJOInstaHashtagsWRAPPER hashtagsList) {
        this.hashtagsList = hashtagsList;
    }

    public ResponsePOJOInstaDesignAndAestheticWRAPPER getDesignAndAestheticList() {
        return designAndAestheticList;
    }

    public void setDesignAndAestheticList(ResponsePOJOInstaDesignAndAestheticWRAPPER designAndAestheticList) {
        this.designAndAestheticList = designAndAestheticList;
    }

    public ResponsePOJOInstaEngagementStrategiesWRAPPER getEngagementStrategiesList() {
        return engagementStrategiesList;
    }

    public void setEngagementStrategiesList(ResponsePOJOInstaEngagementStrategiesWRAPPER engagementStrategiesList) {
        this.engagementStrategiesList = engagementStrategiesList;
    }

    public ResponsePOJOInstaBestPostingTimeWRAPPER getPostingTimeList() {
        return postingTimeList;
    }

    public void setPostingTimeList(ResponsePOJOInstaBestPostingTimeWRAPPER postingTimeList) {
        this.postingTimeList = postingTimeList;
    }
}
