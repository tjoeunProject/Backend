package com.example.project;

import static com.example.project.security.user.Role.ADMIN;
import static com.example.project.security.user.Role.MANAGER;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.project.security.auth.AuthenticationService;
import com.example.project.security.auth.RegisterRequest;



@SpringBootApplication
public class SmartRoutePlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartRoutePlannerApplication.class, args);
	}

	
}
