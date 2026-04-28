package com.ajemi.barber.Ta7li9_app.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "appointments")
@Getter @Setter
public class AppointmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. Chkon l-client?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    // 2. Chkon l-coiffeur?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coiffeur_id", nullable = false)
    private User coiffeur;

    // 3. Ach mn service khtar?
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "appointment_services",
        joinColumns = @JoinColumn(name = "appointment_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<ServiceEntity> services;

    // 4. L-waqt dyal l-appontment
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    

    // 5. L-7ala d l-appointment
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status; // WAITING, IN_PROGRESS, COMPLETED, CANCELLED

    // 6. Ila kān l-client jdid w dkhlo l-coiffeur manual (bla ma i-koun 3ndo compte)
    private String manualClientName;
    private String manualClientPhone;

    // 🔥 Zid hada: L-waqt l-7aqiqi li khdaw les services (b minutes)
    private Integer actualDuration;
}
