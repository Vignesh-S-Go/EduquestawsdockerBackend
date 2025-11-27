package com.vip;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",    // React default
                    "http://localhost:5173",    // Vite default
                    "http://localhost:5174",    // Vite alternative
                    "http://127.0.0.1:5173",    // Alternative localhost
                    "http://127.0.0.1:5174",    // Alternative localhost
                    "http://localhost:3001",
                    "http://localhost:8090",
                    "http://localhost:80"// Alternative React port
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (your frontend URLs)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",      // React default
            "http://localhost:5173",      // Vite default
            "http://localhost:5174",      // Vite alternative
            "http://127.0.0.1:5173",      // Alternative localhost
            "http://127.0.0.1:5174",      // Alternative localhost
            "http://localhost:3001",
            "http://localhost:8090",
            "http://localhost:80"// Alternative React port
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // Allow credentials (important for JWT tokens)
        configuration.setAllowCredentials(true);
        
        // Set max age for preflight requests (1 hour)
        configuration.setMaxAge(3600L);
        
        // Apply CORS configuration to all API endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
