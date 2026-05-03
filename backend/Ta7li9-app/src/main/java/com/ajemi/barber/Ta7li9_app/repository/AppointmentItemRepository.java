package com.ajemi.barber.Ta7li9_app.repository;

import com.ajemi.barber.Ta7li9_app.entity.AppointmentItem;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentItemRepository extends JpaRepository<AppointmentItem, Long> {
// 🔥 L-FIX: Jib liyya akher wa7ed sala, walakin mat-jibch liyya li fih null!
    Optional<AppointmentItem> findFirstByAppointmentClientIdAndServiceIdAndStatusAndActualDurationIsNotNullOrderByEndTimeDesc(
        Long clientId, 
        Long serviceId, 
        String status 
    );
}