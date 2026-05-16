package com.ajemi.barber.Ta7li9_app.service;

import java.time.Duration;
import java.time.LocalDateTime;
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
        List<User> historyClients = appointmentRepository.findMyPastClients(coiffeurId, query);
        
        // 🔥 L-FIX 1: N-filtriw l-History bach n-7iydou l-Coiffeur ila kan fiha
        List<User> filteredHistory = historyClients.stream()
                .filter(client -> !client.getId().equals(coiffeurId))
                .toList();
        
        if (!filteredHistory.isEmpty()) {
            return filteredHistory;
        }

        // Stage 2: Ila malqiti walou f l-History
        if (query.matches("\\d{10}")) { 
            // Kan-mchiw l l-UserRepository n-qelbou b n-nemra
            return userRepository.findByPhoneNumber(query)
                    // 🔥 L-FIX 2: N-checkiw wach had l-user li lqina machi howa l-coiffeur
                    .filter(user -> !user.getId().equals(coiffeurId)) 
                    .map(List::of)
                    .orElse(Collections.emptyList());
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
            coiffeur = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Coiffeur not found"));
            if (coiffeur.getCurrentStatus() != null) {
            String status = coiffeur.getCurrentStatus().name(); // awla coiffeur.getCurrentStatus() ila kant String
            if ("OFFLINE".equals(status)) {
            throw new RuntimeException("BARBER_OFFLINE");
                            }
            }
            if (dto.getClientId() == null && (dto.getManualName() == null || dto.getManualName().trim().isEmpty())) {
                throw new IllegalArgumentException("Ma ymkench t-zid rendez-vous bla klyan w bla smiyat Guest!");
            }
                        if (dto.getClientId() != null) {// 🔥 ZIDNA L-7ARIS HNA: N-checkiw wach had l-klyan li 3zel l-coiffeur dejà chad n-nouba!
                        boolean isBusy = appointmentRepository.existsByClientIdAndStatusIn(
                    dto.getClientId(), 
                    List.of(AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS)
                );
                if (isBusy) {
                    throw new RuntimeException("CLIENT_BUSY"); // Sift l-Error bach y-tchedd f l-front
                }
                // 2. 🔥 L-QALEB J-JDID: N-lghiw (Cancel) ga3 d-demandes PENDING li msifet l-klyan
                List<AppointmentEntity> pendingRequests = appointmentRepository.findByClientIdAndStatus(
                    dto.getClientId(), 
                    AppointmentStatus.PENDING
                );

                for (AppointmentEntity pendingApp : pendingRequests) {
                    pendingApp.setStatus(AppointmentStatus.CANCELLED); // Awla REJECTED 3la 7ssab chno msammiha
                    appointmentRepository.save(pendingApp);
                                        
                    // N-siftou signal l-dok l-7ellaqa lokhrin bach t-t7iyed mn chacha dyalhom f l-blaça!
                    messagingTemplate.convertAndSend("/topic/queue/" + pendingApp.getCoiffeur().getId(), "UPDATE_QUEUE");
                }
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

            // 👇 L-QALEB J-JDID D-HISTORY 👇
            if (client != null) {
                            // ✅ Daba ghadi y-nqqez ga3 dok li fihom null, u y-jib akher wa7ed m-sajjel fih l-weqt s-s7i7!
                java.util.Optional<AppointmentItem> lastTime = appointmentItemRepository
                    .findFirstByAppointmentClientIdAndServiceIdAndStatusAndActualDurationIsNotNullOrderByEndTimeDesc(
                        client.getId(), 
                        srv.getId(), 
                        "COMPLETED"
                    );

                if (lastTime.isPresent() && lastTime.get().getActualDuration() != null) {
                    // ✅ Lqina history: N-khdmou b l-waqt l-m-personnalisé dyalo
                    item.setEstimatedDuration(lastTime.get().getActualDuration());
                } else {
                    // ❌ Awel merra aw ma-3ndouch history: N-3tiweh Default d l-Coiffeur
                    item.setEstimatedDuration(srv.getDuration());
                }
            } else {
                // ❌ Ila kan Guest (Coiffeur dakhlou b yddou bla compte): N-3tiweh Default
                item.setEstimatedDuration(srv.getDuration());
            }
            // 👆 SALA L-QALEB 👆

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
        
        // 🔥 L-QALEB: Blast ma n-jibou l-Default, kan-jem3ou 'estimatedDuration' dyal les items 
        // li 3ad 3mmernaha f l-for loop qbel ma n-3iytou l-had l-fonction
        int totalDuration = app.getAppointmentItems().stream()
                .mapToInt(item -> item.getEstimatedDuration() != null 
                                  ? item.getEstimatedDuration() 
                                  : item.getService().getDuration())
                .sum();

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
        app.setEndTime(startTime.plusMinutes(totalDuration)); // ✅ Hna targetTime ghadi y-koun m-gadd!
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
                other.getAppointmentItems().forEach(item -> item.setStatus("CANCELLED"));
                messagingTemplate.convertAndSend("/topic/queue/" + other.getCoiffeur().getId(), "UPDATE_QUEUE");
            }
        }

        // 2. 🔥 SETUP WAITING & TIME (Hna fine khass t-zid l-khidma)
        app.setStatus(AppointmentStatus.WAITING);
        // 🔥 L-JDID 2: N-reddou les services dyal l-Rendez-vous l-maqboul l WAITING
        app.getAppointmentItems().forEach(item -> item.setStatus("WAITING"));
        // 👈 Kan-jbdou ID dyal s-service mn wst l-Item
        List<Long> serviceIds = app.getAppointmentItems().stream()
                                   .map(item -> item.getService().getId())
                                   .toList();
                                   
        // ✅ CALCULI L-WAQT DYAL N-NOUBA (Darouri!)
        setupQueueTimes(app, app.getCoiffeur().getId(), serviceIds);

        // 3. Save everything
        appointmentRepository.saveAll(otherRequests);
        AppointmentEntity saved = appointmentRepository.saveAndFlush(app);

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
        app.getAppointmentItems().forEach(item -> item.setStatus("CANCELLED"));
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
            dto.setFirstName(entity.getCoiffeur().getFirstName());
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
            
//          🔥 L-FIX: Kan-jbdou estimatedDuration l-m-personnalisée blast l-Default
            int duration = app.getAppointmentItems().stream()
                          .mapToInt(item -> item.getEstimatedDuration() != null 
                                            ? item.getEstimatedDuration() 
                                            : item.getService().getDuration())
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
        LocalDateTime now = LocalDateTime.now();
        System.out.println("DEBUG: Setting StartTime to: " + now);
        app.setStartTime(now); 
        // 🔥 L-FIX 1: N-7esbou total duration b 'estimatedDuration'
        int totalDuration = app.getAppointmentItems().stream()
                .mapToInt(item -> item.getEstimatedDuration() != null 
                                  ? item.getEstimatedDuration() 
                                  : item.getService().getDuration())
                .sum();
        
        // 🔥 L-FIX 2: N-def3ou l-endTime l-gddam (now + duration) bach ma-y-deye3ch l-waqt!
        LocalDateTime newEndTime = now.plusMinutes(totalDuration);
        app.setEndTime(newEndTime);
        // 🔥 L-JDID: Bda ga3 les services f dqa wa7da
        for (AppointmentItem item : app.getAppointmentItems()) {
            if ("WAITING".equals(item.getStatus())|| "PENDING".equals(item.getStatus())) {
                item.setStatus("IN_PROGRESS");
                //item.setStartTime(now);
            }
        }

        AppointmentEntity savedApp = appointmentRepository.saveAndFlush(app);
        updateFutureAppointments(app.getCoiffeur().getId(), newEndTime);
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
        app.setEndTime(now); 

        // 1. N-sajjlou l-waqt t-total dyal l-Rendez-vous
        if (app.getStartTime() != null && app.getClient() != null)  {
            long seconds = Duration.between(app.getStartTime(), now).getSeconds();
            int minutes = (int) Math.ceil(seconds / 60.0);
            app.setActualDuration(minutes);
        }

        // 🔥 L-JDID: N-7esbou chhal mn service baqi IN_PROGRESS wla PENDING
        List<AppointmentItem> remainingItems = app.getAppointmentItems().stream()
                .filter(item -> !"COMPLETED".equals(item.getStatus()))
                .toList();

        // Ila kan baqi GHIR WA7ED, ya3ni rah hwa li kan khddam fih -> N-trackiw weqto
        boolean saveTime = (remainingItems.size() == 1) && (app.getClient() != null);

        // 👇 L-QALEB D-RELATIVE TIME 👇
        // N-qellbou 3la mnin ghadi n-bdaw l-7ssab (Reference Time)
        LocalDateTime referenceTime = app.getStartTime(); // L-Asl howa bdayat l-Rendez-vous
        
        if (saveTime) {
            // N-choufou wach sala chi service 9bel mn hada
            java.util.Optional<AppointmentItem> lastCompletedItem = app.getAppointmentItems().stream()
                    .filter(i -> "COMPLETED".equals(i.getStatus()) && i.getEndTime() != null)
                    .max(java.util.Comparator.comparing(AppointmentItem::getEndTime));
            
            if (lastCompletedItem.isPresent()) {
                referenceTime = lastCompletedItem.get().getEndTime(); // Ila lqa chi service sala, y-bda mn weqto!
            }
        }
        // 👆 SALA L-QALEB 👆

        // 2. N-saliw les services li bqaw
        for (AppointmentItem item : remainingItems) {
            item.setStatus("COMPLETED");
            item.setEndTime(now);
            
            // 🔥 Kan-7sbou b 'referenceTime' blast 'item.getStartTime()'
            if (saveTime && referenceTime != null) {
                long itemSeconds = Duration.between(referenceTime, now).getSeconds();
                int duration = (int) Math.ceil(itemSeconds / 60.0);
                item.setActualDuration(duration);
            }
        }

        appointmentRepository.saveAndFlush(app);
        
        // 3. N-rttbou l-waqt dyal n-nas li kiy-tsennaw mour had l-client
        updateFutureAppointments(app.getCoiffeur().getId(), now);

        // 4. N-checkiw wach l-coiffeur baqi khddam f chi wa7ed khor
        // Optional<AppointmentEntity> baqiKhdam = appointmentRepository
        //         .findTopByCoiffeurIdAndStatusInOrderByEndTimeDesc(
        //             app.getCoiffeur().getId(), 
        //             List.of(AppointmentStatus.IN_PROGRESS)
        //         );

        // if (baqiKhdam.isEmpty() && app.getCoiffeur().getCurrentStatus() == BarberStatus.FULL) {
        //     app.getCoiffeur().setCurrentStatus(BarberStatus.ACTIVE);
        //     userRepository.save(app.getCoiffeur());
        // }
        
        // 5. Notifications
        if (app.getClient() != null) {
            String clientTopic = "/topic/user/" + app.getClient().getId();
            messagingTemplate.convertAndSend(clientTopic, "QUEUE_UPDATED");
        } 
        messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
        
        return mapToResponseDTO(app);
    }
    // had queue li ychofha coiffeur (la2i7at l2intidar active safi)
    public List<AppointmentResponseDTO> getTodayQueue(Long coiffeurId) {
        
        // 1. Jib ghir l-Appointments li m7tajin l-khdma (bla COMPLETED)
        List<AppointmentEntity> activeApps = appointmentRepository.findByCoiffeurIdAndStatusIn(
            coiffeurId, 
            List.of(AppointmentStatus.PENDING, AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS)
        );

        // 2. Rttebhom b l-waqt dyalhom
        List<AppointmentEntity> sortedApps = activeApps.stream()
            .sorted((a, b) -> {
                // Sort: PENDING y-jiw l-foq (wallah 7tarem l-weqt li dejà m-calculé)
                if (a.getStartTime() == null || b.getStartTime() == null) return 0;
                return a.getStartTime().compareTo(b.getStartTime());
            })
            .toList();

        System.out.println("Found Active Appointments: " + sortedApps.size());
        return sortedApps.stream().map(this::mapToResponseDTO).toList();
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
        app.getAppointmentItems().forEach(item -> item.setStatus("CANCELLED"));
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

        LocalDateTime now = LocalDateTime.now();
        AppointmentEntity app = item.getAppointment();
        
        // 👇 L-QALEB D-DAKA2 S-SINA3I 👇
        // 1. N-qellbou 3la akher service sala f had l-Rendez-vous (b l-waqt)
        java.util.Optional<AppointmentItem> lastCompletedItem = app.getAppointmentItems().stream()
                .filter(i -> "COMPLETED".equals(i.getStatus()) && i.getEndTime() != null)
                .max(java.util.Comparator.comparing(AppointmentItem::getEndTime));

        // 2. N-7edou mnin ghadi n-bdaw l-7ssab (Reference Time)
        LocalDateTime referenceTime;
        if (lastCompletedItem.isPresent()) {
            // Ila dejà sala chi service qbel, n-bdaw l-7ssab mnin sala dak s-service
            referenceTime = lastCompletedItem.get().getEndTime();
        } else {
            // Ila hada howa awel service kiy-sali, n-bdaw l-7ssab mn bdayat l-Rendez-vous
            referenceTime = app.getStartTime();
        }

        // 3. N-7sbou l-waqt d had s-service b-dabt
        if (referenceTime != null && app.getClient() != null) {
            long seconds = java.time.Duration.between(referenceTime, now).getSeconds();
            int minutes = (int) Math.ceil(seconds / 60.0);
            item.setActualDuration(minutes);
        }
        // 👆 SALA L-QALEB 👆

        item.setStatus("COMPLETED");
        item.setEndTime(now); 
        appointmentItemRepository.save(item);

        // Daba checki: Wach l-coiffeur salla ga3 les services dyal had l-klyan?
        boolean allDone = app.getAppointmentItems().stream()
                .allMatch(i -> "COMPLETED".equals(i.getStatus()));

        if (allDone) {
            this.completeAppointment(app.getId());
        } else {
            // N-qaddou weqt l-magana l-nas li kiy-tsennaw mourah
            int weqtLiBqa = app.getAppointmentItems().stream()
                    .filter(i -> !"COMPLETED".equals(i.getStatus()))
                    .mapToInt(i -> i.getEstimatedDuration() != null 
                                   ? i.getEstimatedDuration() 
                                   : i.getService().getDuration())
                    .sum();
            
            LocalDateTime expectedEnd = LocalDateTime.now().plusMinutes(weqtLiBqa);
            app.setEndTime(expectedEnd);
            appointmentRepository.saveAndFlush(app); 

            updateFutureAppointments(app.getCoiffeur().getId(), expectedEnd);

            messagingTemplate.convertAndSend("/topic/queue/" + app.getCoiffeur().getId(), "UPDATE_QUEUE");
            if (app.getClient() != null) {
                messagingTemplate.convertAndSend("/topic/user/" + app.getClient().getId(), "QUEUE_UPDATED");
            }
        }
    }
    // 🔥 Jib Requests dyal l-Client (Ghir dyal 24 sa3a li fatet)
    public List<AppointmentResponseDTO> getClientRequests(Long clientId) {
        
        // 1. 7seb l-waqt dyal l-bare7 f nafs l-weqt (Now - 24 hours)
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        // 2. Sifet l-waqt l-Base de données bach t-jib ghir l-jdid
        return appointmentRepository.findByClientIdAndCreatedAtAfterOrderByIdDesc(clientId, twentyFourHoursAgo)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

}
