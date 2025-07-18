package com.MAYA.MAYA.Security;

import com.MAYA.MAYA.Entity.user;
import com.MAYA.MAYA.Repository.userRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class jwtTokenProvider {
    @Value("${secretKey}")
    private String JWT_SECRET ;

    private final long JWT_EXPIRATION_LOGGED = 86400000L; // 1 day

    private final long JWT_EXPIRATION_GUEST = 600000L;
    //120000L for 2 min
    //3600000L for 1 hr
    //600000L;    10 minutes
    //900000L;    15 minutes
    //1800000L;   30 minutes
    //86400000L;  1 day
    @Autowired
    private com.MAYA.MAYA.Repository.userRepository userRepository; // This should be the repository that interacts with your database

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        String role = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse(null);
        System.out.println("auth ye aaya "+role);

        user user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        Long userID = user.getUserId();
        Date now = new Date();
        Date expiryDate = new Date();
        if(role.equalsIgnoreCase("USER")){
        expiryDate = new Date(now.getTime() + JWT_EXPIRATION_LOGGED);}
        else if (role.equalsIgnoreCase("GUEST")) {
            expiryDate = new Date(now.getTime() + JWT_EXPIRATION_GUEST);
        }

        return Jwts.builder()
                .setSubject(email)
                .claim("userID", userID)
                .claim("role",role)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
    }

    public long getUserIDFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token)
                .getBody()
                .get("userID", Long.class); //  Extract the userID claim
    }
    public String getUserRoleFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class); //  Extract the userRole claim
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(JWT_SECRET).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}