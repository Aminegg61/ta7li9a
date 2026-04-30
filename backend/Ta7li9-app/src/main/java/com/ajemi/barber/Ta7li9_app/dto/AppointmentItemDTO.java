package com.ajemi.barber.Ta7li9_app.dto;

import lombok.Data;

@Data
public class AppointmentItemDTO {
    private Long id; // L-ID dyal l-item f tabel jdid (mach ID d service)
    private String serviceName;
    private String status; // PENDING, IN_PROGRESS, COMPLETED
}