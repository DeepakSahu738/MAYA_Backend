package com.MAYA.MAYA.Service.phyllo;

import com.MAYA.MAYA.Entity.instagram.Creator;
import com.MAYA.MAYA.Entity.instagram.Post;
import com.MAYA.MAYA.Repository.instagram.CreatorRepository;
import com.MAYA.MAYA.Repository.instagram.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Computes and persists a creator's data-freshness classification from their
 * current posts. Shared by both the first-connect sync (PhylloSyncService) and
 * the nightly refresh (NightlySyncJob) so the classification is recomputed every
 * time posts change — a creator who was HISTORIC and starts posting again will
 * correctly flip back to RECENT on the next sync.
 *
 *   RECENT   = latest post within 90 days
 *   HISTORIC = has posts but all older than 90 days
 *   STALE    = no posts at all
 *
 * Runs in its own transaction so the update is immediately visible.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataFreshnessService {

    private final CreatorRepository creatorRepository;
    private final PostRepository postRepository;
    private final TransactionTemplate transactionTemplate;

    private static final int RECENT_WINDOW_DAYS = 90;

    public void recompute(Long creatorId) {
        transactionTemplate.executeWithoutResult(status -> {
            Creator c = creatorRepository.findById(creatorId).orElse(null);
            if (c == null) return;

            List<Post> posts = postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId);

            if (posts.isEmpty()) {
                c.setDataFreshness("STALE");
                c.setLatestPostDate(null);
                c.setOldestPostDate(null);
                creatorRepository.save(c);
                log.info("  → Data freshness: STALE (no posts) for creator {}", creatorId);
                return;
            }

            LocalDateTime latestDate = posts.stream()
                .map(Post::getPostedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

            LocalDateTime oldestDate = posts.stream()
                .map(Post::getPostedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

            c.setLatestPostDate(latestDate);
            c.setOldestPostDate(oldestDate);

            if (latestDate != null && latestDate.isAfter(LocalDateTime.now().minusDays(RECENT_WINDOW_DAYS))) {
                c.setDataFreshness("RECENT");
                log.info("  → Data freshness: RECENT (latest post: {}) for creator {}", latestDate, creatorId);
            } else {
                c.setDataFreshness("HISTORIC");
                log.info("  → Data freshness: HISTORIC (latest post: {}) for creator {}", latestDate, creatorId);
            }
            creatorRepository.save(c);
        });
    }
}
