package com.MAYA.MAYA.DTO.facebook.postingTimePojo;

import java.util.List;

public class ResponsePOJOFBPostingTimeWRAPPER {
    private List<ResponsePOJOFBPostingTime>postingTimes;

    public void setPostingTimes(List<ResponsePOJOFBPostingTime> postingTimes) {
        this.postingTimes = postingTimes;
    }

    public List<ResponsePOJOFBPostingTime> getPostingTimes() {
        return postingTimes;
    }
}
