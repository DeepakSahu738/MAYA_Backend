package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Engagement metrics for a piece of content, embedded in {@link Post}.
 *
 * Field names mirror Phyllo's engagement schema so the sync layer, the DB,
 * and the analytics layer all speak the same vocabulary. Every platform
 * populates a different subset — fields that a platform does not report stay
 * null (null means "not returned", NOT zero).
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostMetrics {

    // --- Core engagement (available on most platforms) ---

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    // Nullable — null means data not returned, NOT zero
    @Column(name = "save_count")
    private Integer saveCount;

    // Nullable — null means data not returned, NOT zero
    @Column(name = "share_count")
    private Integer shareCount;

    @Column(name = "repost_count")
    private Integer repostCount;

    // YouTube only
    @Column(name = "dislike_count")
    private Integer dislikeCount;

    // --- Reach / impressions (Instagram, Facebook, Snapchat) ---

    // Nullable — null means data not returned
    @Column(name = "reach_organic_count")
    private Integer reachOrganicCount;

    // Nullable — null means data not returned
    @Column(name = "impression_organic_count")
    private Integer impressionOrganicCount;

    // --- Views / watch time (YouTube, TikTok, Instagram video, Facebook, Twitch, Spotify) ---

    @Column(name = "view_count")
    private Long viewCount;

    // YouTube (IG Direct), Facebook, Spotify
    @Column(name = "watch_time_in_hours")
    private Double watchTimeInHours;

    // Instagram reels
    @Column(name = "avg_watch_time_in_sec")
    private Double avgWatchTimeInSec;

    // Snapchat
    @Column(name = "click_count")
    private Integer clickCount;

    @Column(name = "replay_count")
    private Integer replayCount;

    // --- Maya-computed rates (not from Phyllo) ---

    @Column(name = "engagement_rate")
    private Double engagementRate;

    @Column(name = "save_rate")
    private Double saveRate;

    @Column(name = "share_rate")
    private Double shareRate;
}
