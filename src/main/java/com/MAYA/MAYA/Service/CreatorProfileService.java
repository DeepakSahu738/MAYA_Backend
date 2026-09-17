package com.MAYA.MAYA.Service;

import com.MAYA.MAYA.DTO.creatorprofile.MediaKitResponse;
import com.MAYA.MAYA.Entity.CreatorProfile;
import com.MAYA.MAYA.Entity.UserSocialAccount;
import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Repository.CreatorProfileRepository;
import com.MAYA.MAYA.Repository.UserSocialAccountRepository;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Creator CV / Media Kit logic.
 *
 * Key rules:
 * - Exactly one CreatorProfile per Maya user (get-or-create).
 * - `editedFields` protects user edits: auto-fill-from-sync only writes to fields
 *   the user has NOT manually edited.
 * - The media-kit response merges the stored profile with LIVE per-account stats
 *   pulled from the user's connected Creator rows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreatorProfileService {

    private final CreatorProfileRepository profileRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final CreatorRepository creatorRepository;
    private final AnalyticsService analyticsService;

    /** Get the user's profile, creating an empty one if none exists. */
    @Transactional
    public CreatorProfile getOrCreate(Long userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            CreatorProfile p = new CreatorProfile();
            p.setUserId(userId);
            p.setSource("SYNCED");
            p.setEditedFields(new ArrayList<>());
            CreatorProfile saved = profileRepository.save(p);
            // Best-effort initial fill from any already-connected account
            autoFillFromSync(userId);
            return profileRepository.findByUserId(userId).orElse(saved);
        });
    }

    /** Build the full media-kit response: stored profile + live account stats. */
    @Transactional
    public MediaKitResponse getMediaKit(Long userId) {
        CreatorProfile profile = getOrCreate(userId);

        List<UserSocialAccount> accounts = socialAccountRepository.findByUserId(userId);
        List<MediaKitResponse.ConnectedAccountSummary> summaries = new ArrayList<>();
        long totalFollowers = 0;

        for (UserSocialAccount acc : accounts) {
            if (acc.getCreator() == null) continue;
            Creator c = acc.getCreator();

            Double er = null;
            try {
                var erMetric = analyticsService.calculateOverallEngagement(c.getId(), null);
                er = erMetric != null ? erMetric.getCurrentValue() : null;
            } catch (Exception e) {
                // engagement not computable for this account — leave null
            }

            Integer followers = c.getFollowerCount();
            if (followers != null) totalFollowers += followers;

            summaries.add(new MediaKitResponse.ConnectedAccountSummary(
                c.getId(),
                acc.getPlatform() != null ? acc.getPlatform() : c.getPlatform(),
                acc.getPlatformUsername() != null ? acc.getPlatformUsername() : c.getUsername(),
                followers,
                er,
                c.getIsVerified(),
                c.getProfilePictureUrl(),
                acc.getStatus(),
                acc.getDescription()
            ));
        }

        return new MediaKitResponse(profile, summaries, totalFollowers);
    }

    /**
     * Full update from a user edit. Every non-null field in `incoming` is applied
     * and marked as user-edited (so future syncs won't overwrite it).
     * Passing a field explicitly (even empty) counts as an edit.
     */
    @Transactional
    public CreatorProfile updateProfile(Long userId, CreatorProfile incoming, List<String> changedFields) {
        CreatorProfile p = getOrCreate(userId);

        // Apply incoming values (null = "leave unchanged" for PATCH semantics;
        // the controller decides which fields were actually sent via changedFields)
        applyNonNull(p, incoming);

        // Track edited fields so auto-fill never overwrites them
        Set<String> edited = new LinkedHashSet<>(p.getEditedFields() != null ? p.getEditedFields() : new ArrayList<>());
        if (changedFields != null) edited.addAll(changedFields);
        p.setEditedFields(new ArrayList<>(edited));
        p.setSource("USER_EDITED");
        p.setUpdatedAt(LocalDateTime.now());

        return profileRepository.save(p);
    }

    /**
     * Update the user-provided description of a single connected account.
     * Ownership-checked: the account must belong to userId. Empty string clears it.
     * Returns true if updated, false if the account wasn't found / not owned.
     */
    @Transactional
    public boolean updateAccountDescription(Long userId, Long creatorId, String description) {
        UserSocialAccount account = socialAccountRepository.findByCreatorId(creatorId).orElse(null);
        if (account == null || account.getUserId() == null || !account.getUserId().equals(userId)) {
            return false;
        }
        account.setDescription(description == null || description.isBlank() ? null : description);
        socialAccountRepository.save(account);
        return true;
    }

    /**
     * Auto-fill person-level fields from a connected account's synced data.
     * Only fills fields that are (a) currently empty AND (b) not user-edited.
     * Never destroys user edits. Called after account connect / sync completion.
     */
    @Transactional
    public void autoFillFromSync(Long userId) {
        CreatorProfile p = profileRepository.findByUserId(userId).orElse(null);
        if (p == null) return;

        Set<String> edited = new LinkedHashSet<>(p.getEditedFields() != null ? p.getEditedFields() : new ArrayList<>());

        // Pick a representative connected account (first active one) as the fill source
        List<UserSocialAccount> accounts = socialAccountRepository.findByUserId(userId);
        Creator source = accounts.stream()
            .map(UserSocialAccount::getCreator)
            .filter(c -> c != null)
            .findFirst()
            .orElse(null);
        if (source == null) return;

        boolean changed = false;

        if (isBlank(p.getDisplayName()) && !edited.contains("displayName") && notBlank(source.getUsername())) {
            p.setDisplayName(source.getUsername());
            changed = true;
        }
        if (isBlank(p.getAbout()) && !edited.contains("about") && notBlank(source.getBiography())) {
            p.setAbout(source.getBiography());
            changed = true;
        }
        if (isBlank(p.getPrimaryNiche()) && !edited.contains("primaryNiche") && notBlank(source.getNiche())) {
            p.setPrimaryNiche(source.getNiche());
            changed = true;
        }
        if (isBlank(p.getToneStyle()) && !edited.contains("toneStyle") && notBlank(source.getToneStyle())) {
            p.setToneStyle(source.getToneStyle());
            changed = true;
        }
        if (isBlank(p.getContentGoal()) && !edited.contains("contentGoal") && notBlank(source.getContentGoal())) {
            p.setContentGoal(source.getContentGoal());
            changed = true;
        }
        if (isBlank(p.getTargetAudienceDescription()) && !edited.contains("targetAudienceDescription") && notBlank(source.getTargetAudience())) {
            p.setTargetAudienceDescription(source.getTargetAudience());
            changed = true;
        }
        if (isBlank(p.getBrandPreferences()) && !edited.contains("brandPreferences") && notBlank(source.getBrandPreferences())) {
            p.setBrandPreferences(source.getBrandPreferences());
            changed = true;
        }
        if (isBlank(p.getContactEmail()) && !edited.contains("contactEmail") && notBlank(source.getEmail())) {
            p.setContactEmail(source.getEmail());
            changed = true;
        }
        if (isBlank(p.getProfileImageUrl()) && !edited.contains("profileImageUrl") && notBlank(source.getProfilePictureUrl())) {
            p.setProfileImageUrl(source.getProfilePictureUrl());
            changed = true;
        }

        if (changed) {
            p.setLastSyncedFillAt(LocalDateTime.now());
            // If nothing was user-edited yet, keep source SYNCED; else MIXED
            if (edited.isEmpty()) p.setSource("SYNCED");
            else p.setSource("MIXED");
            profileRepository.save(p);
            log.info("Auto-filled creator profile for user {} from account {}", userId, source.getId());
        }
    }

    /**
     * A concise text summary of the CV for injecting into Maya's chat system
     * message and the weekly strategy prompt. Prefers user-provided data.
     */
    @Transactional(readOnly = true)
    public String buildProfileAiContext(Long userId) {
        CreatorProfile p = profileRepository.findByUserId(userId).orElse(null);
        if (p == null) return "";

        StringBuilder sb = new StringBuilder("CREATOR PROFILE (use to personalize; treat as high-confidence, user-provided):\n");
        if (notBlank(p.getDisplayName())) sb.append("- Name: ").append(p.getDisplayName()).append("\n");
        if (notBlank(p.getHeadline())) sb.append("- Headline: ").append(p.getHeadline()).append("\n");
        if (notBlank(p.getPrimaryNiche())) sb.append("- Niche: ").append(p.getPrimaryNiche()).append("\n");
        if (p.getCategories() != null && !p.getCategories().isEmpty())
            sb.append("- Categories: ").append(String.join(", ", p.getCategories())).append("\n");
        if (notBlank(p.getToneStyle())) sb.append("- Tone: ").append(p.getToneStyle()).append("\n");
        if (notBlank(p.getContentGoal())) sb.append("- Goal: ").append(p.getContentGoal()).append("\n");
        if (notBlank(p.getTargetAudienceDescription())) sb.append("- Target audience: ").append(p.getTargetAudienceDescription()).append("\n");
        if (p.getContentPillars() != null && !p.getContentPillars().isEmpty())
            sb.append("- Content pillars: ").append(String.join(", ", p.getContentPillars())).append("\n");
        if (notBlank(p.getBrandPreferences())) sb.append("- Brand preferences: ").append(p.getBrandPreferences()).append("\n");
        if (p.getAudienceSummary() != null && notBlank(p.getAudienceSummary().getNote()))
            sb.append("- Audience note: ").append(p.getAudienceSummary().getNote()).append("\n");

        // Only return context if we actually have something meaningful
        return sb.length() > 60 ? sb.toString() : "";
    }

    // --- helpers ---

    /**
     * Apply incoming values for PATCH/PUT.
     *
     * String semantics (so the frontend edit-and-save flow can clear fields):
     *   - null  -> leave the stored value UNCHANGED (field was not sent)
     *   - ""    -> CLEAR the stored value (user deleted the text and saved)
     *   - text  -> set the value
     * Non-string fields (lists, objects, booleans) keep null = leave unchanged.
     */
    private void applyNonNull(CreatorProfile target, CreatorProfile in) {
        // String fields — support explicit clear via empty string
        applyString(in.getDisplayName(), target::setDisplayName);
        applyString(in.getHeadline(), target::setHeadline);
        applyString(in.getAbout(), target::setAbout);
        applyString(in.getLocation(), target::setLocation);
        applyString(in.getProfileImageUrl(), target::setProfileImageUrl);
        applyString(in.getPrimaryNiche(), target::setPrimaryNiche);
        applyString(in.getContactEmail(), target::setContactEmail);
        applyString(in.getContactPhone(), target::setContactPhone);
        applyString(in.getPreferredContactMethod(), target::setPreferredContactMethod);
        applyString(in.getDefaultCurrency(), target::setDefaultCurrency);
        applyString(in.getContentGoal(), target::setContentGoal);
        applyString(in.getToneStyle(), target::setToneStyle);
        applyString(in.getTargetAudienceDescription(), target::setTargetAudienceDescription);
        applyString(in.getBrandPreferences(), target::setBrandPreferences);
        applyString(in.getPublicSlug(), target::setPublicSlug);

        // Non-string fields — null means "not sent", so leave unchanged
        if (in.getLanguages() != null) target.setLanguages(in.getLanguages());
        if (in.getCategories() != null) target.setCategories(in.getCategories());
        if (in.getManagementContact() != null) target.setManagementContact(in.getManagementContact());
        if (in.getAudienceSummary() != null) target.setAudienceSummary(in.getAudienceSummary());
        if (in.getRateCard() != null) target.setRateCard(in.getRateCard());
        if (in.getOpenToBarter() != null) target.setOpenToBarter(in.getOpenToBarter());
        if (in.getCollaborations() != null) target.setCollaborations(in.getCollaborations());
        if (in.getTestimonials() != null) target.setTestimonials(in.getTestimonials());
        if (in.getServices() != null) target.setServices(in.getServices());
        if (in.getContentPillars() != null) target.setContentPillars(in.getContentPillars());
        if (in.getIsPublic() != null) target.setIsPublic(in.getIsPublic());
        if (in.getMediaKitAssets() != null) target.setMediaKitAssets(in.getMediaKitAssets());
    }

    /** null = leave unchanged; "" = clear; otherwise set. */
    private void applyString(String value, java.util.function.Consumer<String> setter) {
        if (value == null) return;              // not sent → don't touch
        setter.accept(value.isBlank() ? null : value); // "" → clear, else set
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
