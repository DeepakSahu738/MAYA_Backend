package com.MAYA.MAYA.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.MAYA.MAYA.Repository.userRepository;
import com.MAYA.MAYA.Entity.user;


import java.util.Collections;

@Service
public class CustomUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {


    @Autowired
    private  userRepository userRepository; // This should be the repository that interacts with your database

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        // Fetch user from the database by name
         user user = userRepository.findByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + name));

        // Return the UserDetails object. This is how Spring Security handles the user authentication.
        return new User(user.getName(), user.getPassword(), Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));  // You can add authorities/roles if needed
    }
}
