package com.MAYA.MAYA.Repository;

import com.MAYA.MAYA.Entity.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, Long> {

    // One profile per Maya user
    Optional<CreatorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // For the future public media-kit page (/media-kit/{slug})
    Optional<CreatorProfile> findByPublicSlug(String publicSlug);
}
