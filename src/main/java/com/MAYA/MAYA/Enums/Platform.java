package com.MAYA.MAYA.Enums;

/**
 * Canonical platform identifiers used across sync and analytics.
 *
 * Phyllo (and later our own SDK) reports platform names as free-form strings
 * (e.g. work_platform.name = "Instagram", "YouTube", "Facebook"). Those must be
 * normalized to these canonical values so the analytics layer's per-platform
 * logic can switch reliably. Platforms we don't specifically model fall through
 * to {@link #OTHER}, which uses the Instagram-default analytics behavior.
 */
public enum Platform {

    INSTAGRAM,
    FACEBOOK,
    YOUTUBE,
    OTHER;

    /**
     * Normalizes a raw platform string (from Phyllo work_platform.name, the
     * Creator.platform column, etc.) into a canonical Platform.
     *
     * Case-insensitive and tolerant of minor variants. Unknown / null values
     * map to OTHER (which uses Instagram-default analytics).
     */
    public static Platform normalize(String raw) {
        if (raw == null) return OTHER;
        String v = raw.trim().toUpperCase();
        switch (v) {
            case "INSTAGRAM":
            case "IG":
            case "INSTAGRAM DIRECT":
            case "IG DIRECT":
            case "IG_DIRECT":
            case "INSTAGRAM LITE":
            case "IG LITE":
            case "IG_LITE":
                return INSTAGRAM;
            case "FACEBOOK":
            case "FB":
            case "FACEBOOK COMMERCE":
                return FACEBOOK;
            case "YOUTUBE":
            case "YOU TUBE":
            case "YT":
                return YOUTUBE;
            default:
                return OTHER;
        }
    }
}
