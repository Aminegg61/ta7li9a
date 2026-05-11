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
            // كنقلبو بالـ Phone Number عوض الـ Email
            String adminPhone = "0000000000"; 
            
            if (userRepository.findByPhoneNumber(adminPhone).isEmpty()) {
                User admin = new User();
                admin.setFirstName("Super");
                admin.setLastName("Admin");
                // Password كيحقق الشروط اللي درتي (Uppercase, Lowercase, Number)
                admin.setPassword(encoder.encode("Admin@2026")); 
                admin.setPhoneNumber(adminPhone);
                admin.setRole("ADMIN");
                
                userRepository.save(admin);
                System.out.println("Admin account created with Phone: " + adminPhone + " / Password: Admin@2026");
            } else {
                System.out.println("Admin already exists.");
            }
        };
    }
}
