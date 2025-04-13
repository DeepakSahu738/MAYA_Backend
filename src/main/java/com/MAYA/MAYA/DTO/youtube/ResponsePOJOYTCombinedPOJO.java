package com.MAYA.MAYA.DTO.youtube;

import com.MAYA.MAYA.DTO.youtube.engagementPojo.ResponsePOJOYTEngagementWRAPPER;
import com.MAYA.MAYA.DTO.youtube.hashtagPojo.ResponsePOJOYTHashtagWRAPPER;
import com.MAYA.MAYA.DTO.youtube.postingTime.ResponsePOJOYTPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.youtube.seoPojo.ResponsePOJOYTSEOWRAPPER;
import com.MAYA.MAYA.DTO.youtube.thumbnailAndBranding.ResponsePOJOYTThumbnailAndBrandingWRAPPER;
import com.MAYA.MAYA.DTO.youtube.videoIdeaPojo.ResponsePOJOYTVideoIdeaWRAPPER;

public class ResponsePOJOYTCombinedPOJO {
    private ResponsePOJOYTVideoIdeaWRAPPER videoIdeaList;
    private ResponsePOJOYTSEOWRAPPER seoList;
    private ResponsePOJOYTHashtagWRAPPER hashtagList;
    private ResponsePOJOYTThumbnailAndBrandingWRAPPER thumbnailAndBrandingList;
    private ResponsePOJOYTEngagementWRAPPER engagementList;
    private ResponsePOJOYTPostingTimeWRAPPER postingTimeList;

    public ResponsePOJOYTVideoIdeaWRAPPER getVideoIdeaList() {
        return videoIdeaList;
    }

    public void setVideoIdeaList(ResponsePOJOYTVideoIdeaWRAPPER videoIdeaList) {
        this.videoIdeaList = videoIdeaList;
    }

    public ResponsePOJOYTSEOWRAPPER getSeoList() {
        return seoList;
    }

    public void setSeoList(ResponsePOJOYTSEOWRAPPER seoList) {
        this.seoList = seoList;
    }

    public ResponsePOJOYTHashtagWRAPPER getHashtagList() {
        return hashtagList;
    }

    public void setHashtagList(ResponsePOJOYTHashtagWRAPPER hashtagList) {
        this.hashtagList = hashtagList;
    }

    public ResponsePOJOYTThumbnailAndBrandingWRAPPER getThumbnailAndBrandingList() {
        return thumbnailAndBrandingList;
    }

    public void setThumbnailAndBrandingList(ResponsePOJOYTThumbnailAndBrandingWRAPPER thumbnailAndBrandingList) {
        this.thumbnailAndBrandingList = thumbnailAndBrandingList;
    }

    public ResponsePOJOYTEngagementWRAPPER getEngagementList() {
        return engagementList;
    }

    public void setEngagementList(ResponsePOJOYTEngagementWRAPPER engagementList) {
        this.engagementList = engagementList;
    }

    public ResponsePOJOYTPostingTimeWRAPPER getPostingTimeList() {
        return postingTimeList;
    }

    public void setPostingTimeList(ResponsePOJOYTPostingTimeWRAPPER postingTimeList) {
        this.postingTimeList = postingTimeList;
    }
}
