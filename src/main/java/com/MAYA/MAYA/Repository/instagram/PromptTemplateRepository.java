package com.MAYA.MAYA.Repository.instagram;

import com.MAYA.MAYA.Entity.instagram.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    Optional<PromptTemplate> findByTemplateKeyAndIsActive(String templateKey, Boolean isActive);
    List<PromptTemplate> findByPlatformAndIsActive(String platform, Boolean isActive);
    List<PromptTemplate> findByCategoryAndIsActive(String category, Boolean isActive);
}
