package com.ajemi.barber.Ta7li9_app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.ajemi.barber.Ta7li9_app.dto.ServiceRequestDTO;
import com.ajemi.barber.Ta7li9_app.dto.ServiceResponseDTO;
import com.ajemi.barber.Ta7li9_app.entity.AppointmentItem;
import com.ajemi.barber.Ta7li9_app.entity.ServiceEntity;
import com.ajemi.barber.Ta7li9_app.entity.User;
import com.ajemi.barber.Ta7li9_app.repository.AppointmentItemRepository;
import com.ajemi.barber.Ta7li9_app.repository.ServiceRepository;
import com.ajemi.barber.Ta7li9_app.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ServicesBarberService {
    
    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppointmentItemRepository appointmentItemRepository;

    public ServiceResponseDTO addService(ServiceRequestDTO dto, Long coiffeurId) {
        User coiffeur = userRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur not found"));

        ServiceEntity service = new ServiceEntity();
        service.setName(dto.getName());
        service.setPrice(dto.getPrice());
        service.setDuration(dto.getDuration());
        service.setCoiffeur(coiffeur);

        ServiceEntity saved = serviceRepository.save(service);
        return mapToResponse(saved);
    }

    public List<ServiceResponseDTO> getCoiffeurServices(Long coiffeurId) {
        return serviceRepository.findByCoiffeurIdAndDeletedFalse(coiffeurId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ServiceResponseDTO updateService(Long serviceId, ServiceRequestDTO dto, Long coiffeurId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("Service ma-lqinahch!"));
        // 2. IMPORTANT: Check wach had l-coiffeur li m-connecté (coiffeurId) 
        // howa nfsou mol had l-service (service.getCoiffeur().getId())
        if (!service.getCoiffeur().getId().equals(coiffeurId)) {
           throw new AccessDeniedException("Ma-3ndekch l-7eqq t-beddel had l-service, machi dyalk!");
        }
        // 3. Modifi l-ma3loumat (Mapping DTO -> Entity)
        if (dto.getName() != null) service.setName(dto.getName());
        if (dto.getPrice() != null) service.setPrice(dto.getPrice());
        if (dto.getDuration() != null) service.setDuration(dto.getDuration());

        // 4. Save f l-base de données
        ServiceEntity updated = serviceRepository.save(service);
        return mapToResponse(updated);
    }
    @Transactional
    public void deleteService(Long serviceId, Long coiffeurId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("Service ma-lqinahch!"));
        
        if (!service.getCoiffeur().getId().equals(coiffeurId)) {
            throw new AccessDeniedException("Ma-3ndekch l-7eqq t-msse7 had l-service!");
        }
        service.setDeleted(true);
        
        // serviceRepository.delete(service);
    }


    private ServiceResponseDTO mapToResponse(ServiceEntity entity) {
        return new ServiceResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                formatDuration(entity.getDuration()),
                entity.getCoiffeur().getFirstName() + " " + entity.getCoiffeur().getLastName()
        );
    }

    private String formatDuration(Integer minutes) {
        if (minutes == null) return "0min";
        if (minutes < 60) return minutes + "min";
        
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        
        if (remainingMinutes == 0) {
            return hours + "h";
        }
        return hours + "h " + remainingMinutes + "min";
    }
    @Transactional
    public List<Map<String, Object>> getClientCustomServices(Long barberId, Long clientId) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 1. Njibou les services dyal l-coiffeur
        List<ServiceEntity> barberServices = serviceRepository.findByCoiffeurId(barberId); 

        // 2. N-souwlou l-Database
        for (ServiceEntity srv : barberServices) {
            
            Optional<AppointmentItem> latestItem = appointmentItemRepository
                .findFirstByAppointmentClientIdAndServiceIdAndStatusAndActualDurationIsNotNullOrderByEndTimeDesc(
                    clientId, 
                    srv.getId(), 
                    "COMPLETED"
                );

            latestItem.ifPresent(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("serviceId", srv.getId());
                map.put("customDuration", item.getActualDuration());
                result.add(map);
            });
        }

        return result;
    }
}
