package com.ajemi.barber.Ta7li9_app.repository;

import com.ajemi.barber.Ta7li9_app.entity.AppointmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentItemRepository extends JpaRepository<AppointmentItem, Long> {
}