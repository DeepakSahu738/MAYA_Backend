package com.MAYA.MAYA.DTO.snapchat;

import com.MAYA.MAYA.DTO.snapchat.EngagementFeaturesPojo.ResponsePOJOSCEngagementFeaturesWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.boostingTipsPojo.ResponsePOJOSCBoostingTipsWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.filtersPojo.ResponsePOJOSClensesAndFiltersWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.postingTimePojo.ResponsePOJOSCPOstingTimeWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.storyIdeaPojo.ResponsePOJOSCStoryIdeaWRAPPER;
import com.MAYA.MAYA.DTO.snapchat.textOverlaysPojo.ResponsePOJOSCTextOverlaysWRAPPER;

public class ResponsePOJOSCConbinedPOJO {
    private ResponsePOJOSCStoryIdeaWRAPPER storyIdeaList;
    private ResponsePOJOSCTextOverlaysWRAPPER textOverlaysList;
    private ResponsePOJOSClensesAndFiltersWRAPPER lensesAndFiltersList;
    private ResponsePOJOSCEngagementFeaturesWRAPPER engagementFeaturesList;
    private ResponsePOJOSCBoostingTipsWRAPPER boostingTipsList;
    private ResponsePOJOSCPOstingTimeWRAPPER postingTimeList;

    public ResponsePOJOSCStoryIdeaWRAPPER getStoryIdeaList() {
        return storyIdeaList;
    }

    public void setStoryIdeaList(ResponsePOJOSCStoryIdeaWRAPPER storyIdeaList) {
        this.storyIdeaList = storyIdeaList;
    }

    public ResponsePOJOSCTextOverlaysWRAPPER getTextOverlaysList() {
        return textOverlaysList;
    }

    public void setTextOverlaysList(ResponsePOJOSCTextOverlaysWRAPPER textOverlaysList) {
        this.textOverlaysList = textOverlaysList;
    }

    public ResponsePOJOSClensesAndFiltersWRAPPER getLensesAndFiltersList() {
        return lensesAndFiltersList;
    }

    public void setLensesAndFiltersList(ResponsePOJOSClensesAndFiltersWRAPPER lensesAndFiltersList) {
        this.lensesAndFiltersList = lensesAndFiltersList;
    }

    public ResponsePOJOSCEngagementFeaturesWRAPPER getEngagementFeaturesList() {
        return engagementFeaturesList;
    }

    public void setEngagementFeaturesList(ResponsePOJOSCEngagementFeaturesWRAPPER engagementFeaturesList) {
        this.engagementFeaturesList = engagementFeaturesList;
    }

    public ResponsePOJOSCBoostingTipsWRAPPER getBoostingTipsList() {
        return boostingTipsList;
    }

    public void setBoostingTipsList(ResponsePOJOSCBoostingTipsWRAPPER boostingTipsList) {
        this.boostingTipsList = boostingTipsList;
    }

    public ResponsePOJOSCPOstingTimeWRAPPER getPostingTimeList() {
        return postingTimeList;
    }

    public void setPostingTimeList(ResponsePOJOSCPOstingTimeWRAPPER postingTimeList) {
        this.postingTimeList = postingTimeList;
    }
}
