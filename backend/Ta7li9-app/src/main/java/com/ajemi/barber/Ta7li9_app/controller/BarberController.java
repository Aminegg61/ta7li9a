package com.ajemi.barber.Ta7li9_app.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ajemi.barber.Ta7li9_app.security.UserPrincipal;
import com.ajemi.barber.Ta7li9_app.service.BarberService;


@RestController
@RequestMapping("/api/barber")
@PreAuthorize("hasRole('COIFFEUR')")
public class BarberController {
    @Autowired private BarberService barberService;
    @PutMapping("/status")
    public ResponseEntity<Void> updateStatus(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @RequestParam String status) {
            barberService.toggleStatus(currentUser.getId(), status);
            return ResponseEntity.ok().build();
        }
     
    @GetMapping("/status")
    // @PreAuthorize("hasRole('COIFFEUR')")
    public ResponseEntity<Map<String, Object>> getStatus(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> statusData = barberService.getBarberStatus(currentUser.getId());
        return ResponseEntity.ok(statusData);
    }    

    @PostMapping("/pause")
    public ResponseEntity<?> pauseWork(@AuthenticationPrincipal UserPrincipal currentUser) {
        // Kan-3iyytou l-Service li qaddina qbila
        barberService.pauseWork(currentUser.getId());
        return ResponseEntity.ok().body("{\"message\": \"Coiffeur is now ON BREAK\"}");
    }

    @PostMapping("/resume")
    public ResponseEntity<?> resumeWork(@AuthenticationPrincipal UserPrincipal currentUser) {
        // Kan-3iyytou l-Service
        barberService.resumeWork(currentUser.getId());
        return ResponseEntity.ok().body("{\"message\": \"Coiffeur is BACK TO WORK\"}");
    }

}
