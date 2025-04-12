package com.MAYA.MAYA.DTO.tiktok;

import com.MAYA.MAYA.DTO.tiktok.EngagementPojo.ResponsePOJOTTEngagementWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.hashtagPojo.ResponsePOJOTTHashtagsWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.hooksAndCaptionsPojo.ResponsePOJOTTHooksAndCaptionsWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.musicAndEffectPojo.ResponsePOJOTTMusicAndEffetWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.postingTimePojo.ResponsePOJOTTPostingTimeWRAPPER;
import com.MAYA.MAYA.DTO.tiktok.videoIdeaPojo.ResponsePOJOTTVideoIdeaWRAPPER;

public class ResponsePOJOTTCombinedPOJO {
    private ResponsePOJOTTVideoIdeaWRAPPER videoIdeaList;
    private ResponsePOJOTTHooksAndCaptionsWRAPPER hooksAndCaptionsList;
    private ResponsePOJOTTHashtagsWRAPPER hashtagsList;
    private ResponsePOJOTTMusicAndEffetWRAPPER musicAndEffetList;
    private ResponsePOJOTTEngagementWRAPPER engagementTacticList;
    private ResponsePOJOTTPostingTimeWRAPPER postingTimeList;

    public ResponsePOJOTTVideoIdeaWRAPPER getVideoIdeaList() {
        return videoIdeaList;
    }

    public void setVideoIdeaList(ResponsePOJOTTVideoIdeaWRAPPER videoIdeaList) {
        this.videoIdeaList = videoIdeaList;
    }

    public ResponsePOJOTTHooksAndCaptionsWRAPPER getHooksAndCaptionsList() {
        return hooksAndCaptionsList;
    }

    public void setHooksAndCaptionsList(ResponsePOJOTTHooksAndCaptionsWRAPPER hooksAndCaptionsList) {
        this.hooksAndCaptionsList = hooksAndCaptionsList;
    }

    public ResponsePOJOTTHashtagsWRAPPER getHashtagsList() {
        return hashtagsList;
    }

    public void setHashtagsList(ResponsePOJOTTHashtagsWRAPPER hashtagsList) {
        this.hashtagsList = hashtagsList;
    }

    public ResponsePOJOTTMusicAndEffetWRAPPER getMusicAndEffetList() {
        return musicAndEffetList;
    }

    public void setMusicAndEffetList(ResponsePOJOTTMusicAndEffetWRAPPER musicAndEffetList) {
        this.musicAndEffetList = musicAndEffetList;
    }

    public ResponsePOJOTTEngagementWRAPPER getEngagementTacticList() {
        return engagementTacticList;
    }

    public void setEngagementTacticList(ResponsePOJOTTEngagementWRAPPER engagementTacticList) {
        this.engagementTacticList = engagementTacticList;
    }

    public ResponsePOJOTTPostingTimeWRAPPER getPostingTimeList() {
        return postingTimeList;
    }

    public void setPostingTimeList(ResponsePOJOTTPostingTimeWRAPPER postingTimeList) {
        this.postingTimeList = postingTimeList;
    }
}
