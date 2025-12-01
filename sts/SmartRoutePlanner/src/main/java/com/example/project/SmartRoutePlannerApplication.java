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

	// 임의로 Admin 을 추가한 케이스 안써도 됨
	@Bean
	public CommandLineRunner commandLineRunner(
			AuthenticationService service
	) {
		return args -> {
			var admin = RegisterRequest.builder()
					.firstname("Admin")
					.lastname("Admin")
					.email("admin@mail.com")
					.password("password")
					.role(ADMIN)
					.build();
			System.out.println("Admin token: " + service.register(admin).getAccessToken());

			var manager = RegisterRequest.builder()
					.firstname("Admin")
					.lastname("Admin")
					.email("manager@mail.com")
					.password("password")
					.role(MANAGER)
					.build();
			System.out.println("Manager token: " + service.register(manager).getAccessToken());

		};
	}
}
