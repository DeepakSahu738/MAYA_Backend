package com.MAYA.MAYA.Entity;

import com.MAYA.MAYA.DTO.creatorprofile.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Creator CV / Media Kit — ONE per Maya user (person-level, spans all their
 * connected platform accounts).
 *
 * Design:
 * - Simple person-level fields are plain columns.
 * - Structured/list fields (testimonials, rate card, collaborations, etc.) are
 *   JSONB columns mapped via @JdbcTypeCode(SqlTypes.JSON). This makes adding/
 *   removing sub-fields a code-only change (new DTO field) with NO DB migration.
 * - `editedFields` records which fields the user manually changed, so the
 *   auto-fill-from-sync logic never overwrites user edits, and recommendations
 *   can trust user-edited data over (unreliable) synced data.
 * - Per-account live stats (followers, ER per platform) are NOT stored here —
 *   they're computed at read time from the user's linked Creator rows so the
 *   media kit always reflects the current set of connected accounts.
 */
@Entity
@Table(name = "creator_profiles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "creator_profile_seq")
    @SequenceGenerator(name = "creator_profile_seq", sequenceName = "creator_profiles_id_seq", allocationSize = 50)
    private Long id;

    // ONE profile per Maya user
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // --- Identity / headline ---
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "headline")
    private String headline;              // one-liner, e.g. "Fitness creator & coach"

    @Column(name = "about", columnDefinition = "TEXT")
    private String about;

    @Column(name = "location")
    private String location;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages", columnDefinition = "jsonb")
    private List<String> languages;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "primary_niche")
    private String primaryNiche;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories", columnDefinition = "jsonb")
    private List<String> categories;

    // --- Contact / deal info ---
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "management_contact", columnDefinition = "jsonb")
    private ManagementContact managementContact;

    @Column(name = "preferred_contact_method")
    private String preferredContactMethod; // EMAIL | PHONE | DM

    // --- Audience (aggregated across accounts) ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_summary", columnDefinition = "jsonb")
    private AudienceSummary audienceSummary;

    // --- Deal / commercial ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rate_card", columnDefinition = "jsonb")
    private List<RateCardItem> rateCard;

    @Column(name = "default_currency")
    private String defaultCurrency;

    @Column(name = "open_to_barter")
    private Boolean openToBarter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "collaborations", columnDefinition = "jsonb")
    private List<Collaboration> collaborations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "testimonials", columnDefinition = "jsonb")
    private List<Testimonial> testimonials;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "services", columnDefinition = "jsonb")
    private List<ServiceOffering> services;

    // --- Creator preferences (feed Maya personalization) ---
    @Column(name = "content_goal")
    private String contentGoal;

    @Column(name = "tone_style")
    private String toneStyle;

    @Column(name = "target_audience_description", columnDefinition = "TEXT")
    private String targetAudienceDescription;

    @Column(name = "brand_preferences", columnDefinition = "TEXT")
    private String brandPreferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_pillars", columnDefinition = "jsonb")
    private List<String> contentPillars;

    // --- Media kit / sharing ---
    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "public_slug", unique = true)
    private String publicSlug;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_kit_assets", columnDefinition = "jsonb")
    private MediaKitAssets mediaKitAssets;

    // --- Provenance & housekeeping ---
    // Overall provenance hint: SYNCED | USER_EDITED | MIXED
    @Column(name = "source")
    private String source = "SYNCED";

    // Names of fields the user has manually edited — protected from auto-fill overwrite
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edited_fields", columnDefinition = "jsonb")
    private List<String> editedFields;

    @Column(name = "last_synced_fill_at")
    private LocalDateTime lastSyncedFillAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
