package com.ajemi.barber.Ta7li9_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer"; // Hada standard f JWT
    private String role;

    // Constructeur sghir ila bghiti t-rjje3 ghir l-token f l-bdaya
    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }
}
