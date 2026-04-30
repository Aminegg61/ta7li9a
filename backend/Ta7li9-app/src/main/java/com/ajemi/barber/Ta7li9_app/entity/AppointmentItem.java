package com.ajemi.barber.Ta7li9_app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hadi kat-rbet had l-item m3a l-Rendez-vous l-kbir
    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private AppointmentEntity appointment;

    // Hadi kat-rbet had l-item m3a s-service (7sana, L7ya...)
    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    // L-7ala dyal had s-service bo7dou: PENDING, IN_PROGRESS, COMPLETED
    private String status = "PENDING"; 

    // L-waqt foqach bda w foqach sala had s-service b dbt
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Ch7al khda dyal l-waqt b d-dqiqa
    private Integer actualDuration; 
}
