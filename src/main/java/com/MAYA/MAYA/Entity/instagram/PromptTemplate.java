package com.MAYA.MAYA.Entity.instagram;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String templateKey;
    
    @Column(nullable = false)
    private String platform;
    
    @Column(nullable = false)
    private String category;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String promptText;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    private Integer version = 1;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
}
