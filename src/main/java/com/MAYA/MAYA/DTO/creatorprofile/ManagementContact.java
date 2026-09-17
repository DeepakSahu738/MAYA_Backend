package com.MAYA.MAYA.DTO.creatorprofile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agency/manager contact for brand deals.
 * Stored as the creator_profile.management_contact JSONB object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagementContact {
    private String name;      // manager or agency name
    private String email;
    private String phone;
    private String agency;
}
