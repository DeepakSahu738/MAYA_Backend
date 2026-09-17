package com.MAYA.MAYA.DTO.creatorprofile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single testimonial in the creator's media kit. Stored inside the
 * creator_profile.testimonials JSONB array. Fields can be added/removed here
 * with no DB migration — old rows simply lack the new key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Testimonial {
    private String author;      // "John Doe"
    private String role;        // "Marketing Lead"
    private String brand;       // "BrandX"
    private String quote;       // the testimonial text
    private String avatarUrl;   // optional
    private Integer rating;     // optional, 1-5
}
