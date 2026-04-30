package com.ajemi.barber.Ta7li9_app.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ajemi.barber.Ta7li9_app.dto.AppointmentItemDTO;
import com.ajemi.barber.Ta7li9_app.dto.AppointmentRequestDTO;
import com.ajemi.barber.Ta7li9_app.dto.AppointmentResponseDTO;
import com.ajemi.barber.Ta7li9_app.entity.AppointmentEntity;
import com.ajemi.barber.Ta7li9_app.entity.AppointmentItem;
import com.ajemi.barber.Ta7li9_app.entity.AppointmentStatus;
import com.ajemi.barber.Ta7li9_app.entity.BarberStatus;
import com.ajemi.barber.Ta7li9_app.entity.ServiceEntity;
import com.ajemi.barber.Ta7li9_app.entity.User;
import com.ajemi.barber.Ta7li9_app.repository.AppointmentItemRepository;
import com.ajemi.barber.Ta7li9_app.repository.AppointmentRepository;
import com.ajemi.barber.Ta7li9_app.repository.ServiceRepository;
import com.ajemi.barber.Ta7li9_app.repository.UserRepository;
import com.ajemi.barber.Ta7li9_app.security.UserPrincipal;

@Service
public class AppointmentService {
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired 
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private AppointmentItemRepository appointmentItemRepository;

    public List<User> searchClient(Long coiffeurId, String query) {
        // Stage 1: Qelleb f l-History dyalk (Prefix Search)
        // Ila ktabتي "A", kiy-jbed Amine li fayt 7ssen 3ndek   
        List<User> historyClients = appointmentRepository.findMyPastClients(coiffeurId, query);
        
        if (!historyClients.isEmpty()) {
            return historyClients;
        }

        // Stage 2: Ila malqiti walou f l-History (ya3ni jdid 3ndek)
        // Khass darori i-koun ktab nemra d t-telfon kamla (masalan 10 d l-arqam)
        if (query.matches("\\d{10}")) { 
            // Kan-mchiw l l-UserRepository n-qelbou f l-app kamla
            return userRepository.findByPhoneNumber(query)
                    .map(List::of) // Ila lqah kiy-rj3o f Lista
                    .orElse(Collections.emptyList()); // Ila malqahch kiy-rjje3 lista khawya
        }

        return Collections.emptyList();
    }
    
@Transactional
    public AppointmentResponseDTO createAppointment(UserPrincipal currentUser, AppointmentRequestDTO dto) {
        Long currentUserId = currentUser.getId();
        boolean isCoiffeur = currentUser.isCoiffeur();
        
        // 1. Check Duplicate l l-Client (Zid PENDING l l-lista dyal l-verif)
        if (!isCoiffeur) {
            boolean alreadyInQueue = appointmentRepository.existsByClientIdAndStatusIn(
                currentUserId, 
                List.of( AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS)
            );
            if (alreadyInQueue) {
                throw new RuntimeException("Rak dejà f n-nouba walla katti-tsenna acceptation!");
            }
        }

        AppointmentEntity appointment = new AppointmentEntity();
        User coiffeur;
        User client = null;
        String manualName = null;

        // --- Ta7did Roles ---
        if (isCoiffeur) { // Coiffeur Manual Add
            coiffeur = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Coiffeur not found"));
            if (dto.getClientId() != null) {
                client = userRepository.findById(dto.getClientId()).orElse(null);
            } else {
                manualName = dto.getManualName();
            }
            // Manual add kiy-koun direct WAITING
            appointment.setStatus(AppointmentStatus.WAITING);
        } 
        else { // Client Send Demand
            coiffeur = userRepository.findById(dto.getBarberId()).orElseThrow(() -> new RuntimeException("Coiffeur not found"));
            client = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Client context not found"));
            
            // Demand kiy-koun PENDING u weqt khawi
            appointment.setStatus(AppointmentStatus.PENDING);
            appointment.setStartTime(null);
            appointment.setEndTime(null);
        }

        // 🔥 Hna jm3na l-Items bach y-t-criyaw l-Client w l-Coiffeur b-jouj
        List<ServiceEntity> services = serviceRepository.findAllById(dto.getServiceIds());
        List<AppointmentItem> items = new java.util.ArrayList<>();

        for (ServiceEntity srv : services) {
            AppointmentItem item = new AppointmentItem();
            item.setAppointment(appointment);
            item.setService(srv);
            item.setStatus("PENDING"); 
            items.add(item);
        }
        
        // Lsse9hom f l-Appointment l-kbir
        appointment.setAppointmentItems(items);

        // 🔥 3ad n-7sbou l-waqt d n-nouba ila kan Coiffeur 
        if (isCoiffeur) {
            setupQueueTimes(appointment, coiffeur.getId(), dto.getServiceIds());
        }

        appointment.setCoiffeur(coiffeur);
        appointment.setClient(client);
        appointment.setManualClientName(manualName);

        AppointmentEntity savedApp = appointmentRepository.save(appointment);
        appointmentRepository.flush();
        
        messagingTemplate.convertAndSend("/topic/queue/" + coiffeur.getId(), "UPDATE_QUEUE");
        
        // 2. Notify l-Client (ILA KAN registered user, ya3ni 3ndou clientID)
        if (appointment.getClient() != null) {
            String clientTopic = "/topic/user/" + appointment.getClient().getId();
            messagingTemplate.convertAndSend(clientTopic, "QUEUE_UPDATED");
        }
        
        return mapToResponseDTO(savedApp);
    }

    // Helper method bach mat-3awdch l-code dyal calculation
    private void setupQueueTimes(AppointmentEntity app, Long coiffeurId, List<Long> serviceIds) {
        List<ServiceEntity> services = serviceRepository.findAllById(serviceIds);
        int totalDuration = services.stream().mapToInt(ServiceEntity::getDuration).sum();

        // 1. Filter: ghir WAITING u IN_PROGRESS
        List<AppointmentStatus> activeStatuses = List.of(AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS);
        
        // 2. Query
        Optional<AppointmentEntity> lastApp = appointmentRepository
                .findTopByCoiffeurIdAndStatusInOrderByEndTimeDesc(coiffeurId, activeStatuses);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;

        // 3. Safety Check: darouri n-takkdou beli endTime machi null
        if (lastApp.isPresent() && lastApp.get().getEndTime() != null) {
            LocalDateTime lastEndTime = lastApp.get().getEndTime();
            // Ila akhir rdv salla qbel dba, bda dba. Ila baqi kheddam, bda m3ah.
            startTime = lastEndTime.isAfter(now) ? lastEndTime : now;
        } else {
            // Ma-lqina walou walla l-rdv l-akhir khawi (null), bda dba
            startTime = now;
        }

        app.setStartTime(startTime);
        app.setEndTime(startTime.plusMinutes(totalDuration));
    }

@Transactional
    public AppointmentResponseDTO acceptAppointment(Long appointmentId) {
        AppointmentEntity app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Demand ma-lqinahch"));

        Long clientId = app.getClient().getId();

        // 1. AUTO-CANCEL (hadchi kheddam mzyan)
        List<AppointmentEntity> otherRequests = appointmentRepository
                .findByClientIdAndStatus(clientId, AppointmentStatus.PENDING);
        
        for (AppointmentEntity other : otherRequests) {
            if (!other.getId().equals(appointmentId)) {
                other.setStatus(AppointmentStatus.CANCELLED);
                messagingTemplate.convertAndSend("/topic/queue/" + other.getCoiffeur().getId(), "UPDATE_QUEUE");
            }
        }

        // 2. 🔥 SETUP WAITING & TIME (Hna fine khass t-zid l-khidma)
        app.setStatus(AppointmentStatus.WAITING);
        
        // 👈 Kan-jbdou ID dyal s-service mn wst l-Item
        List<Long> serviceIds = app.getAppointmentItems().stream()
                                   .map(item -> item.getService().getId())
                                   .toList();
                                   
        // ✅ CALCULI L-WAQT DYAL N-NOUBA (Darouri!)
        setupQueueTimes(app, app.getCoiffeur().getId(), serviceIds);

        // 3. Save everything
        appointmentRepository.saveAll(otherRequests);
        AppointmentEntity saved = appointmentRepository.save(app);

        // 4. Notify l-Client u l-Coiffeur
        messagingTemplate.convertAndSend("/topic/user/" + clientId, "QUEUE_UPDATED");
        messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");

        return mapToResponseDTO(saved);
    }
    @Transactional
    public void rejectAppointment(Long appointmentId,UserPrincipal currentUser ) {
        AppointmentEntity app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        // --- Security Check ---
        // Ila klyian, khassu y-koun hwa moul l-appointment
        if (currentUser.isClient() && !app.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Ma-3ndkch l-7aq t-annuler had l-appointment!");
        }
        
        // Ila coiffeur, khassu y-koun hwa l-coiffeur m-assigné
        if (currentUser.isCoiffeur() && !app.getCoiffeur().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Had l-appointment machi dyalk!");
        }
        app.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(app);
        
        messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
        // 🔥 Signal l-Client: Bach l-bouton trje3 "Send Demand" f-lblassa
        if (app.getClient() != null) {
            messagingTemplate.convertAndSend("/topic/user/" + app.getClient().getId(), "QUEUE_UPDATED");
        }
    }
    private AppointmentResponseDTO mapToResponseDTO(AppointmentEntity entity) {
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setId(entity.getId());
        
        if (entity.getCoiffeur() != null) {
            dto.setBarberId(entity.getCoiffeur().getId());
        }
        // Logic dyal s-smiya: User official wala Manual
        if (entity.getClient() != null) {
            dto.setClientId(entity.getClient().getId());
            dto.setClientName(entity.getClient().getFirstName() + " " + entity.getClient().getLastName());
        } else {
            dto.setClientId(null);
            dto.setClientName(entity.getManualClientName());
        }

        // 👈 Kan-jbdou s-service mn wst l-Item
        dto.setServiceNames(entity.getAppointmentItems().stream()
                .map(item -> item.getService().getName())
                .toList());
                
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setStatus(entity.getStatus().toString());
            // Hna t7seb total duration b minutes
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            Duration duration = Duration.between(entity.getStartTime(), entity.getEndTime());
            dto.setTotalDuration((int) duration.toMinutes()); // 7ssab minutes
        } else {
            dto.setTotalDuration(0); // ila ma kaynach dates
        }
        // 🔥 Jbed l-items u 3merhom f DTO jdid bach Angular y-chouf kolchi m-fereq
        List<AppointmentItemDTO> itemDtos = entity.getAppointmentItems().stream().map(item -> {
            AppointmentItemDTO itemDto = new AppointmentItemDTO();
            itemDto.setId(item.getId());
            itemDto.setServiceName(item.getService().getName());
            itemDto.setStatus(item.getStatus());
            return itemDto;
        }).toList();
        
        dto.setItems(itemDtos);

        return dto;
    }
    //hadi 3la hssab updat time ila t3atal coiffeur
    @Transactional
    public void updateFutureAppointments(Long coiffeurId, LocalDateTime newEndTime) {
        // 1. Jib ga3 n-nas li kiy-tsennaw (WAITING) m-rttbin b start time, bla man-choufo wach l-waqt fet awla la
        List<AppointmentEntity> futureApps = appointmentRepository
                .findByCoiffeurIdAndStatusOrderByStartTimeAsc(
                    coiffeurId, 
                    AppointmentStatus.WAITING
                );

        LocalDateTime currentPointer = newEndTime;

        for (AppointmentEntity app : futureApps) {
            // 2. Start time d s-sayed jdid = End time d s-sayed li qbel mennu
            app.setStartTime(currentPointer);
            
        // 👈 Kan-jbdou Duration d s-service mn wst l-Item
        int duration = app.getAppointmentItems().stream()
                          .mapToInt(item -> item.getService().getDuration())
                          .sum();
            app.setEndTime(currentPointer.plusMinutes(duration));
            
            // 4. Pointer kiy-mchi l l-mou3id li jay
            currentPointer = app.getEndTime();
        }
        appointmentRepository.saveAll(futureApps);
    }
    //button start mn yabda
@Transactional
    public AppointmentResponseDTO startAppointment(Long appointmentId) {
        AppointmentEntity app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment malqinahch"));
                
        app.setStatus(AppointmentStatus.IN_PROGRESS);
        
        // 🔥 Zid had l-log bach t-choufi f l-console wach l-waqt t-set-a s7i7
        LocalDateTime now = LocalDateTime.now();
        System.out.println("DEBUG: Setting StartTime to: " + now);
        app.setStartTime(now); 

        // 🔥 Ista3mly saveAndFlush bach t-sauvaw l-updates deghya
        AppointmentEntity savedApp = appointmentRepository.saveAndFlush(app);
        
        // Notification real-time (Darouri bach l-client y-chouf l-bedil)
        messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
        
        return mapToResponseDTO(savedApp);
    }

    //button done ila sala
    @Transactional
    public AppointmentResponseDTO completeAppointment(Long appointmentId) {
        AppointmentEntity app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment malqinahch"));
                
        app.setStatus(AppointmentStatus.COMPLETED);
        LocalDateTime now = LocalDateTime.now();
        app.setEndTime(now); // Salla dba
        // 👇 ZID HADI 1: N-7sbou ch7al d l-weqt khda b-s-sa7 w n-sajloha f DB
        if (app.getStartTime() != null) {
            long minutes = Duration.between(app.getStartTime(), now).toMinutes();
            app.setActualDuration((int) minutes);
        }

        appointmentRepository.saveAndFlush(app);
        
        // 👇 ZID HADI 2: N-rttbou l-waqt dyal n-nas li kiy-tsennaw mour had l-client
        updateFutureAppointments(app.getCoiffeur().getId(), now);
        // 👇 3. 🔥 HNA ZIDNA L-LOGIC DYAL STATUS L-JDIDA 🔥 👇
        // N-checkiw wach l-coiffeur baqi khddam f chi wa7ed khor (IN_PROGRESS)
        Optional<AppointmentEntity> baqiKhdam = appointmentRepository
                .findTopByCoiffeurIdAndStatusInOrderByEndTimeDesc(
                    app.getCoiffeur().getId(), 
                    List.of(AppointmentStatus.IN_PROGRESS)
                );

        // Ila l-korsi khawi daba, u l-coiffeur kan status dyalo FULL, n-reddouh ACTIVE bo7dou!
        if (baqiKhdam.isEmpty() && app.getCoiffeur().getCurrentStatus() == BarberStatus.FULL) {
            app.getCoiffeur().setCurrentStatus(BarberStatus.ACTIVE);
            userRepository.save(app.getCoiffeur());
        }
        // 👆 ========================================= 👆
    if (app.getClient() != null) {
            System.out.println("Sending signal to user: " + app.getClient().getId()); // <--- CHOUF WACH KAT-TLA3 HADI
            String clientTopic = "/topic/user/" + app.getClient().getId();
            messagingTemplate.convertAndSend(clientTopic, "QUEUE_UPDATED");
        } else {
            System.out.println("Client is NULL - No signal sent"); // <--- ILA TLA3AT HADI, RA L-ENTITY MA-FIHACH CLIENT
        }
        messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
        return mapToResponseDTO(appointmentRepository.save(app));
    }
    // had queeue li ychofha coiffeur la2i7at l2intidar 
    public List<AppointmentResponseDTO> getTodayQueue(Long coiffeurId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // 1. Jib ga3 l-Appointments dyal had l-coiffeur (ga3 l-statuses li bghina)
        List<AppointmentEntity> allApps = appointmentRepository.findByCoiffeurIdAndStatusIn(
            coiffeurId, 
            List.of(AppointmentStatus.PENDING, AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)
        );

        // 2. Filter-i l-data f Java bach t-7ell mouchkil l-NULL u l-Dates
        List<AppointmentEntity> filteredApps = allApps.stream()
            .filter(app -> {
                // A: Ila kan PENDING, khallih y-douz (wakha startTime null)
                if (app.getStatus() == AppointmentStatus.PENDING) return true;
                
                // B: Ila kan status khor, khass darouri y-koun f lyoma
                if (app.getStartTime() != null) {
                    return !app.getStartTime().isBefore(startOfDay) && !app.getStartTime().isAfter(endOfDay);
                }
                return false;
            })
            .sorted((a, b) -> {
                // Sort: PENDING y-jiw l-foq (wallah 7tarem l-weqt li dejà m-calculé)
                if (a.getStartTime() == null || b.getStartTime() == null) return 0;
                return a.getStartTime().compareTo(b.getStartTime());
            })
            .toList();

        System.out.println("Found Appointments: " + filteredApps.size());
        return filteredApps.stream().map(this::mapToResponseDTO).toList();
    }

    public List<AppointmentResponseDTO> getMyActiveAppointments(Long clientId) {
        // 1. Statuses li bghina n-suiviw
        List<AppointmentStatus> activeStatuses = List.of(
            AppointmentStatus.WAITING, 
            AppointmentStatus.IN_PROGRESS,
            AppointmentStatus.PENDING
        );

        // 2. ⚡ Beddelna findTop b findBy bach n-jebdo koulchi
        return appointmentRepository.findByClientIdAndStatusIn(clientId, activeStatuses)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    // 🔥 Button Clear: Kat-mssa7 klyan mn n-nouba u katti-gad l-waqt l-nas li morah
    @Transactional
    public void clearAppointment(Long appointmentId) {
        AppointmentEntity app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment malqinahch"));
        
        // 1. Check wach l-klyan baqi WAITING aw PENDING (Ma-ymknch n-ms7o wahed IN_PROGRESS)
        if (app.getStatus() != AppointmentStatus.WAITING && app.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Ma-ymknch t-clear-i wahed m-bedi (In Progress) wla msali!");
        }

        // 2. Beddel Status l-CANCELLED
        app.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.saveAndFlush(app);
        
        // 3. Jib l-waqt mnin ghadi t-bda n-nouba jdida
        // N-choufo wach l-coiffeur khddam f chi wahed daba (IN_PROGRESS)
        Optional<AppointmentEntity> inProgressApp = appointmentRepository
                .findTopByCoiffeurIdAndStatusInOrderByEndTimeDesc(
                    app.getCoiffeur().getId(), 
                    List.of(AppointmentStatus.IN_PROGRESS)
                );
                
        // Ila kan khddam f chi wahed, n-nouba ghadi t-bda melli y-sali m3ah (getEndTime).
        // Ila makanch (coiffeur jales), n-nouba ghadi t-bda mn daba (now).
        LocalDateTime nextStartTime = inProgressApp.map(AppointmentEntity::getEndTime).orElse(LocalDateTime.now());
        
        // 4. Qadd l-weqt l-nas li bqaw f n-nouba (Recalculate)
        updateFutureAppointments(app.getCoiffeur().getId(), nextStartTime);
        
        // 5. Sift Notifications l-Front-end
        messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
        
        if (app.getClient() != null) {
            messagingTemplate.convertAndSend("/topic/user/" + app.getClient().getId(), "QUEUE_UPDATED");
        }
    }
    // 🔥 Button Start l-kola Service bo7dou
    @Transactional
    public void startSingleService(Long itemId) {
        AppointmentItem item = appointmentItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item malqinahch"));

        item.setStatus("IN_PROGRESS");
        item.setStartTime(LocalDateTime.now()); // Waqt bdayat s-service

        AppointmentEntity app = item.getAppointment();
        
        // Ila kan hada awl service bda fih l-coiffeur m3a had l-klyan
        // Khassna n-bdelou status dyal l-Rendez-vous kaml l-IN_PROGRESS
        if (app.getStatus() != AppointmentStatus.IN_PROGRESS) {
            app.setStatus(AppointmentStatus.IN_PROGRESS);
            app.setStartTime(LocalDateTime.now());
            appointmentRepository.save(app);
        }

        appointmentItemRepository.save(item);

        // Sift Signal bach l-App t-updata Live
        messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
    }

    // 🔥 Button Done l-kola Service bo7dou
    @Transactional
    public void completeSingleService(Long itemId) {
        AppointmentItem item = appointmentItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item malqinahch"));

        item.setStatus("COMPLETED");
        LocalDateTime now = LocalDateTime.now();
        item.setEndTime(now); // Waqt nihayat s-service

        // 7seb l-waqt l-7aqiqi d had s-service bo7dou
        if (item.getStartTime() != null) {
            long minutes = Duration.between(item.getStartTime(), now).toMinutes();
            item.setActualDuration((int) minutes);
        }
        appointmentItemRepository.save(item);

        AppointmentEntity app = item.getAppointment();

        // Daba checki: Wach l-coiffeur salla ga3 les services dyal had l-klyan?
        boolean allDone = app.getAppointmentItems().stream()
                .allMatch(i -> "COMPLETED".equals(i.getStatus()));

        if (allDone) {
            // 🎉 Ila salaw kamlin, 3iyyet l-completeAppointment l-qdima (li katti-sali l-Rendez-vous u katti-qadd n-nouba)
            this.completeAppointment(app.getId());
        } else {
            // Ila baqi lih chi service akhor (maslan salla 7sana u baqya L7ya)
            messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
        }
    }
}
