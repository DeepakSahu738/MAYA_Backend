package com.MAYA.MAYA.Repository;

import com.MAYA.MAYA.Entity.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    // Get all connected accounts for a Maya user
    List<UserSocialAccount> findByUserIdAndStatus(Long userId, String status);

    // Get all accounts for a user (any status)
    List<UserSocialAccount> findByUserId(Long userId);

    // Find by Phyllo user ID (to check if we already created a Phyllo user for this Maya user)
    Optional<UserSocialAccount> findFirstByUserId(Long userId);

    // Find by Phyllo account ID (after SDK callback)
    Optional<UserSocialAccount> findByPhylloAccountId(String phylloAccountId);

    // Find by user + platform
    List<UserSocialAccount> findByUserIdAndPlatform(Long userId, String platform);
}
