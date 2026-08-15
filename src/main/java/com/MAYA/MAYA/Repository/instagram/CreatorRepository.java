package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, Long> {
    Optional<Creator> findByInstagramId(String instagramId);
    Optional<Creator> findByUsername(String username);
}
