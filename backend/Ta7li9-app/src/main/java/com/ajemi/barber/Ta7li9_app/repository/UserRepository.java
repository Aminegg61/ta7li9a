package com.ajemi.barber.Ta7li9_app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ajemi.barber.Ta7li9_app.entity.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);
    Boolean existsByPhoneNumber(String phone);
    Optional<User> findByPhoneNumber(String phoneNumber);

    //search barber-----------------------------------------------------------------------------------
        @Query("SELECT u FROM User u WHERE u.role = 'COIFFEUR' AND (" +
            "LOWER(CONCAT(u.firstName, ' ', u.lastName)) = LOWER(TRIM(:query)) OR " +
            "u.phoneNumber = TRIM(:query))")
        List<User> searchBarbers(@Param("query") String query);
        @Query("SELECT u FROM User u WHERE u.role = 'COIFFEUR' AND (" +
       // A. Exact Match (l-ga3 l-nas)
       "LOWER(CONCAT(u.firstName, ' ', u.lastName)) = LOWER(TRIM(:query)) OR " +
       "LOWER(CONCAT(u.lastName, ' ', u.firstName)) = LOWER(TRIM(:query)) OR " +
       "u.phoneNumber = TRIM(:query) OR " +
       
       // B. Prefix Match (Ghi l-nas li déjà suivi)
       "(u.id IN :followedIds AND (" +
           "LOWER(u.firstName) LIKE LOWER(CONCAT(TRIM(:query), '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT(TRIM(:query), '%')) OR " +
           "u.phoneNumber LIKE CONCAT(TRIM(:query), '%')" +
       "))" +
       ")")
        List<User> searchBarbersHybrid(@Param("query") String query, @Param("followedIds") List<Long> followedIds);
    
}
