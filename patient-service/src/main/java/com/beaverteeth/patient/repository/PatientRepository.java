package com.beaverteeth.patient.repository;

import com.beaverteeth.patient.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPhone(String phone);
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByTelegramUsername(String telegramUsername);

    List<Patient> findByFullNameContainingIgnoreCase(String name);
    List<Patient> findByIsActiveTrue();
    Optional<Patient> findByIdAndIsActiveTrue(Long id);

    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByTelegramUsername(String telegramUsername);
}