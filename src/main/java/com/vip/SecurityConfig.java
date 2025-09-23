package com.vip;

import com.vip.JwtRequestFilter; // --- NEW: Import the JWT filter
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy; // --- NEW
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // --- NEW
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter; // --- NEW: Inject your custom JWT filter

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // --- MODIFIED: Authorize requests
            .authorizeHttpRequests(requests -> requests
                // Public endpoints
                .requestMatchers("/api/auth/**", "/api/users/register", "/api/users/login").permitAll()
                .requestMatchers("/h2-console/**").permitAll() // If you use H2
                // All other requests must be authenticated
                .anyRequest().authenticated()
            )
            // --- NEW: Set session management to STATELESS
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // --- NEW: Add the JWT filter before the standard username/password filter
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
            // --- This part is for H2 console, keep if you use it
            .headers(headers -> headers.frameOptions().disable());
            
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- NEW: Expose the AuthenticationManager as a Bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (your frontend URLs)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",      // React default
            "http://localhost:5173",      // Vite default
            "http://localhost:5174",
            "http://localhost:8989",	  //  Vite alternative
            "http://ec2-13-201-34-207.ap-south-1.compute.amazonaws.com"
        ));
        
        // IMPORTANT: Allow the "Authorization" header
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        
        // Allow all standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Apply CORS configuration
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all paths
        
        return source;
    }
}
