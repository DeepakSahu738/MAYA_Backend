package com.MAYA.MAYA.DTO.facebook;

import com.MAYA.MAYA.DTO.facebook.adAndBoostingPojo.ResponsePOJOFBadAndBoostingWRAPPER;
import com.MAYA.MAYA.DTO.facebook.engagementFeaturePojo.ResponsePOJOFBengagementFeatureWRAPPER;
import com.MAYA.MAYA.DTO.facebook.hashtagsPojo.ResponsePOJOFBhashtagsWRAPPER;
import com.MAYA.MAYA.DTO.facebook.headlinesAndDesPojo.ResponsePOJOFBheadlinesAndDesWRAPPER;
import com.MAYA.MAYA.DTO.facebook.postIdeaPojo.ResponsePOJOFBPostIdeaWRAPPER;
import com.MAYA.MAYA.DTO.facebook.postingTimePojo.ResponsePOJOFBPostingTimeWRAPPER;

public class ResponsePOJOFBConbinedPOJO {
    private ResponsePOJOFBPostIdeaWRAPPER postIdeasList;
    private ResponsePOJOFBheadlinesAndDesWRAPPER headlinesAndDesList;
    private ResponsePOJOFBengagementFeatureWRAPPER engagementFeatureList;
    private ResponsePOJOFBhashtagsWRAPPER hashtagsList;
    private ResponsePOJOFBadAndBoostingWRAPPER adAndBoostingList;
    private ResponsePOJOFBPostingTimeWRAPPER PostingTimeList;

    public ResponsePOJOFBPostIdeaWRAPPER getPostIdeasList() {
        return postIdeasList;
    }

    public void setpostIdeasList(ResponsePOJOFBPostIdeaWRAPPER postIdeasList) {
        this.postIdeasList = postIdeasList;
    }

    public ResponsePOJOFBheadlinesAndDesWRAPPER getheadlinesAndDesList() {
        return headlinesAndDesList;
    }

    public void setheadlinesAndDesList(ResponsePOJOFBheadlinesAndDesWRAPPER headlinesAndDesList) {
        this.headlinesAndDesList = headlinesAndDesList;
    }

    public ResponsePOJOFBengagementFeatureWRAPPER getengagementFeatureList() {
        return engagementFeatureList;
    }

    public void setengagementFeatureList(ResponsePOJOFBengagementFeatureWRAPPER engagementFeatureList) {
        this.engagementFeatureList = engagementFeatureList;
    }

    public ResponsePOJOFBhashtagsWRAPPER gethashtagsList() {
        return hashtagsList;
    }

    public void sethashtagsList(ResponsePOJOFBhashtagsWRAPPER hashtagsList) {
        this.hashtagsList = hashtagsList;
    }

    public ResponsePOJOFBadAndBoostingWRAPPER getadAndBoostingList() {
        return adAndBoostingList;
    }

    public void setadAndBoostingList(ResponsePOJOFBadAndBoostingWRAPPER adAndBoostingList) {
        this.adAndBoostingList = adAndBoostingList;
    }

    public ResponsePOJOFBPostingTimeWRAPPER getPostingTimeList() {
        return PostingTimeList;
    }

    public void setPostingTimeList(ResponsePOJOFBPostingTimeWRAPPER PostingTimeList) {
        this.PostingTimeList = PostingTimeList;
    }
}
