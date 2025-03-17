package com.MAYA.MAYA.Repository;

import com.MAYA.MAYA.Entity.user;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface userRepository extends JpaRepository<user, Long> {
    Optional<user> findByName(String name); ;
}
