package com.ajemi.barber.Ta7li9_app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ajemi.barber.Ta7li9_app.entity.User;
import com.ajemi.barber.Ta7li9_app.repository.UserRepository;

@SpringBootApplication
public class Ta7li9AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(Ta7li9AppApplication.class, args);
	}

	@Bean
	CommandLineRunner initAdmin(UserRepository userRepository, BCryptPasswordEncoder encoder) {
		return args -> {
			if (userRepository.findByEmail("admin@ta7li9a.com").isEmpty()) {
				User admin = new User();
				admin.setFirstName("Super");
				admin.setLastName("Admin");
				admin.setEmail("admin@ta7li9a.com");
				admin.setPassword(encoder.encode("Admin@2026")); // Password qwi
				admin.setPhoneNumber("0000011111");
				admin.setRole("ADMIN");
				userRepository.save(admin);
				System.out.println("Admin account created: admin@ta7li9a.com / Admin@2026");
			}
		};
	}

}
