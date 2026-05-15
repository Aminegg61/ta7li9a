package com.ajemi.barber.Ta7li9_app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ajemi.barber.Ta7li9_app.dto.BarberSearchDto;
import com.ajemi.barber.Ta7li9_app.security.UserPrincipal;
import com.ajemi.barber.Ta7li9_app.service.ManageBarberService;
import com.ajemi.barber.Ta7li9_app.service.ServicesBarberService;

@RestController
@RequestMapping("/api/barbers")
@PreAuthorize("hasRole('CLIENT')")
public class ManageBarberForClientController {
    @Autowired
    private ManageBarberService manageBarberService;
    @Autowired 
    private ServicesBarberService servicesBarberService;
    @GetMapping("/search")
    public ResponseEntity<List<BarberSearchDto>> searchBarbers(@RequestParam("q") String query,
        @AuthenticationPrincipal UserPrincipal currentUser) {
        List<BarberSearchDto> results = manageBarberService.findBarberForClient(query,currentUser.getId());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/add-barber/{barberId}")
    public ResponseEntity<String> addBarber(
        @AuthenticationPrincipal UserPrincipal currentUser, 
        @PathVariable Long barberId) {
        
        manageBarberService.addBarberToMyList(currentUser.getId(), barberId);
        return ResponseEntity.ok("Barber t-zad l d-dashboard dyalk!");
    }

    @GetMapping("/my-barbers")
    public ResponseEntity<List<BarberSearchDto>> getMyBarbers(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<BarberSearchDto> myBarbers = manageBarberService.getMyBarbers(currentUser.getId());
        return ResponseEntity.ok(myBarbers);
    }

    @GetMapping("/my-favorites")
    public ResponseEntity<List<BarberSearchDto>> getMyFavorites(@AuthenticationPrincipal UserPrincipal currentUser) {
        // L-favorites bo7dhom
        List<BarberSearchDto> favorites = manageBarberService.getMyFavoriteBarbers(currentUser.getId());
        return ResponseEntity.ok(favorites);
    }

    @DeleteMapping("/remove-barber/{barberId}")
    public ResponseEntity<Void> removeBarber(
            @AuthenticationPrincipal UserPrincipal currentUser, 
            @PathVariable Long barberId) {
        
        manageBarberService.removeBarberFromMyList(currentUser.getId(), barberId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/toggle-favorite/{barberId}")
    public ResponseEntity<Void> toggleFavorite(
            @AuthenticationPrincipal UserPrincipal currentUser, 
            @PathVariable Long barberId) {
        
        manageBarberService.toggleFavoriteStatus(currentUser.getId(), barberId);
        return ResponseEntity.ok().build();
    }
    // 🔥 Hadi dyal l-Client: ki-jbed biha l-awqat l-mkhasssa dyalo 3nd chi 7ellaq
    @GetMapping("/my-custom-times/{barberId}")
    public ResponseEntity<List<Map<String, Object>>> getMyCustomTimes(
            @PathVariable Long barberId,
            @AuthenticationPrincipal UserPrincipal currentUser) { // Hna currentUser howa l-Client
            
        // Kanhdmo b nfs l-methode li sayebna 9bila! (barberId, clientId)
        List<Map<String, Object>> customDurations = 
                servicesBarberService.getClientCustomServices(barberId, currentUser.getId());
                
        return ResponseEntity.ok(customDurations);
    }
}
