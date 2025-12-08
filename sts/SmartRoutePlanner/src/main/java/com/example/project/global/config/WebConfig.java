package com.example.project.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**") 
                .allowedOrigins(
                		
                        "http://localhost:5173",  // Vite (React)
                        "http://172.16.250.69:5173",
                        "http://172.16.250.69:5173",
                        "http://localhost:3000"   // CRA (혹시 사용하는 경우)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true);
    }
}
