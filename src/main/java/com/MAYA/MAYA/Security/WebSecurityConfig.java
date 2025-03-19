package com.MAYA.MAYA.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${secretKey}")
    private String JWT_SECRET ;
    private final jwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public WebSecurityConfig(jwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/auth/login", "/auth/registerUser").permitAll()
                        .requestMatchers("/auth/**").authenticated()
                        .requestMatchers("/api/content/**").permitAll()

                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt((jwt) -> jwt.decoder(jwtDecoder()))
                );
        return http.build();
    }

      @Bean
      public JwtDecoder jwtDecoder() {
      // Use a secret key to decode the JWT (make sure the key matches the one you use to sign the JWT)
          String secretKey = JWT_SECRET; // secret key
          byte[] keyBytes = Base64.getDecoder().decode(secretKey);  // Decode your key if it's Base64 encoded
          SecretKey secretKeyObj = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");  // Create a SecretKey from your byte array
          return NimbusJwtDecoder.withSecretKey(secretKeyObj).build();
            }

    // Autowire AuthenticationManager bean (Spring will provide this automatically)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        System.out.println("AuthenticationManager bean created: " + authenticationConfiguration.getAuthenticationManager());
        return authenticationConfiguration.getAuthenticationManager();
    }



}