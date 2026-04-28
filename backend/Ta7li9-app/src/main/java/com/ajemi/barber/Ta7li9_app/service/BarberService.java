package com.ajemi.barber.Ta7li9_app.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ajemi.barber.Ta7li9_app.entity.AppointmentEntity;
import com.ajemi.barber.Ta7li9_app.entity.AppointmentStatus;
import com.ajemi.barber.Ta7li9_app.entity.BarberStatus;
import com.ajemi.barber.Ta7li9_app.entity.User;
import com.ajemi.barber.Ta7li9_app.repository.AppointmentRepository;
import com.ajemi.barber.Ta7li9_app.repository.UserRepository;

@Service
public class BarberService {
    @Autowired private UserRepository userRepository;
    // ⚡ Had l-outil houwa li kiy-sift l-messages f WebSocket
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @Autowired private AppointmentRepository appointmentRepository;

    @Transactional
    public void toggleStatus(Long coiffeurId, String newStatus) {
        BarberStatus status = fromString(newStatus);
        
        // 1. Update f l-Base de données
        User coiffeur = userRepository.findById(coiffeurId).get();
        coiffeur.setCurrentStatus(status);
        userRepository.save(coiffeur);

        // 2. Sift l-išara l-qdima (Khelliha ila knti baqi katti-sta3melha f chi blassa)
        String topic = "/topic/status/" + coiffeurId;
        messagingTemplate.convertAndSend(topic, newStatus.toUpperCase());
        
        // 👇 3. 🔥 ZID HADI: Sift Signal f Queue bach l-App jdida dyal l-client t-sme3 w t-updata l-alwan
        messagingTemplate.convertAndSend("/topic/queue/" + coiffeurId, "STATUS_CHANGED");
    }

    private  static BarberStatus fromString(String value) {
        return BarberStatus.valueOf(value.toUpperCase());
    }
    public String getBarberStatus(Long coiffeurId) {
        User barber = userRepository.findById(coiffeurId)
            .orElseThrow(() -> new RuntimeException("Barber ma-lqinahch!"));
        
        // Suwwel rassek: wach l-status smitou 'currentStatus' walla 'status' f l-Entity dyalk?
        // 3la 7sab l-logs li sifti qbel, rak msemih 'current_status'
        return barber.getCurrentStatus().toString(); 
    }

    // 🔥 Hadi hiyya l-logic li ghat-tla3 l-Client f Front-end
    public String calculateStatusForClient(User barber) {
        
        // 1. L-Coiffeur sed l-ma7al b yddou (Offline)
        if (barber.getCurrentStatus() == BarberStatus.OFFLINE) {
            return "CLOSED"; 
        }
        
        // 2. L-Coiffeur dar FULL b yddou (3amar w ma-bghach y-zido nas)
        if (barber.getCurrentStatus() == BarberStatus.FULL) {
            return "FULL"; 
        }

        // 3. Ila kan ACTIVE, hna kiy-tdkhl l-logic dyal n-nouba (Auto-Status)
        if (barber.getCurrentStatus() == BarberStatus.ACTIVE) {
            
            // Jbed n-nouba dyal lyoma
            List<AppointmentEntity> activeQueue = appointmentRepository.findByCoiffeurIdAndStatusIn(
                barber.getId(), 
                List.of(AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS)
            );

            // A: 0 in queue == OPEN
            if (activeQueue.isEmpty()) {
                return "OPEN"; 
            }

            // B: N-checkiw wach 3ndou chi wa7ed m-bdi (IN_PROGRESS)
            boolean hasInProgress = activeQueue.stream()
                .anyMatch(app -> app.getStatus() == AppointmentStatus.IN_PROGRESS);

            // C: having someone in queue but didn't hit start (or just hit stop) == ON BREAK
            if (!hasInProgress) {
                return "ON_BREAK"; 
            } else {
                // D: 3ndou nas w zaydon khddam f chi wa7ed daba f l-korsi
                return "BUSY"; 
            }
        }

        return "CLOSED"; // Par defaut ila wqe3 chi mouchkil
    }

}
