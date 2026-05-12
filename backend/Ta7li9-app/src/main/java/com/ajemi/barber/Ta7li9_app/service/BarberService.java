package com.ajemi.barber.Ta7li9_app.service;

import java.time.Duration;
import java.time.LocalDateTime;
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
        
        User coiffeur = userRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur malqinahch"));

        // 🔥 1. Logic dyal FULL: Ma-tqderch tdirha w n-nouba khawya
        if (status == BarberStatus.FULL) {
            List<AppointmentEntity> activeQueue = appointmentRepository.findByCoiffeurIdAndStatusIn(
                coiffeurId, 
                List.of(AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS)
            );
            
            if (activeQueue.isEmpty()) {
                // Hada ghay-rejje3 Error 400 l-Angular
                throw new IllegalStateException("Ma tqderch tdir FULL w n-nouba khawya!"); 
            }
        }

        // 🔥 2. Logic dyal OFFLINE: Annuler ga3 l-klyan li kyt-snaw (WAITING) wla li talbin (PENDING)
        if (status == BarberStatus.OFFLINE) {
            List<AppointmentEntity> toCancel = appointmentRepository.findByCoiffeurIdAndStatusIn(
                coiffeurId, 
                List.of(AppointmentStatus.WAITING, AppointmentStatus.PENDING)
            );
            
            if (!toCancel.isEmpty()) {
                toCancel.forEach(app -> {
                    // Mola7ada: T2ked blli l-Enum dyalek smitou CANCELED, awla bdelha l-REJECTED ila knti msemiha hakka
                    app.setStatus(AppointmentStatus.CANCELLED); 
                });
                appointmentRepository.saveAll(toCancel);
            }
            
            // Ila kan chi wa7ed f l-korsi (IN_PROGRESS), ghay-bqa hta y-sali m3ah l-coiffeur b yddou.
        }

        // 3. Update Status f l-Base de données
        coiffeur.setCurrentStatus(status);
        userRepository.save(coiffeur);

        // 4. Sift l-išara f WebSocket (Hadi ghay-sme3ha l-klyan w l-coiffeur bach y-t-updataw)
        String topic = "/topic/status/" + coiffeurId;
        messagingTemplate.convertAndSend(topic, newStatus.toUpperCase());
        
        // Zidna signal d l-queue bach ta l-klyan li t-annula y-t-refrisha 3ndo l-UI
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

        // 🔥 L-JDID: L-Coiffeur dar Pause b yddou (Manual Pause)
        if (barber.getCurrentStatus() == BarberStatus.ON_BREAK) {
            return "ON_BREAK";
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
    @Transactional
    public void pauseWork(Long coiffeurId) {
        User coiffeur = userRepository.findById(coiffeurId)
                .orElseThrow(() -> new RuntimeException("Coiffeur malqinahch"));
        
        // Reddo f wqt r-raha f DB
        coiffeur.setCurrentStatus(BarberStatus.ON_BREAK);
        coiffeur.setLastPauseTime(LocalDateTime.now()); 
        userRepository.saveAndFlush(coiffeur);
        
        // Sift Update l-Klyan bach l-Magana t-dir Pause w HTML y-tbeddel
        messagingTemplate.convertAndSend("/topic/queue/" + coiffeurId, "STATUS_CHANGED");
    }

    @Transactional
    public void resumeWork(Long coiffeurId) {
        User coiffeur = userRepository.findById(coiffeurId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        if (coiffeur.getLastPauseTime() != null) {
            // 🔥 N-7esbou chhal d-twani t-pawza fihom
            long pauseSeconds = Duration.between(coiffeur.getLastPauseTime(), now).getSeconds();
            
            // Jbed l-klyan li f l-korsi (IN_PROGRESS)
            appointmentRepository.findByCoiffeurIdAndStatusIn(coiffeurId, List.of(AppointmentStatus.IN_PROGRESS))
                .stream().findFirst().ifPresent(currentApt -> {
                    
                    // 1. Zid l-waqt d-Pause f l-endTime d l-Rendez-vous (Bach magana ma-t-khrbqch)
                    currentApt.setEndTime(currentApt.getEndTime().plusSeconds(pauseSeconds));
                    
                    // 🔥 2. L-FIX D-HISTORY: Zid l-waqt d-pause f 'startTime' dyal les services li IN_PROGRESS
                    // Hakka l-backend ma-ghadich y-7seb l-waqt d-pause f l-history d l-klyan!
                    currentApt.getAppointmentItems().stream()
                        .filter(item -> "IN_PROGRESS".equals(item.getStatus()) && item.getStartTime() != null)
                        .forEach(item -> {
                            item.setStartTime(item.getStartTime().plusSeconds(pauseSeconds));
                        });
                        
                    appointmentRepository.save(currentApt);
                });
        }

        coiffeur.setCurrentStatus(BarberStatus.ACTIVE);
        coiffeur.setLastPauseTime(null); 
        userRepository.saveAndFlush(coiffeur);
        
        messagingTemplate.convertAndSend("/topic/queue/" + coiffeurId, "STATUS_CHANGED");
    }
}
