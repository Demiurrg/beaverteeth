package com.beaverteeth.doctor.repository;

import com.beaverteeth.doctor.model.Doctor;
import com.beaverteeth.doctor.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecialty(Specialty specialty);

    List<Doctor> findByFullNameContainingIgnoreCase(String name);

    List<Doctor> findByIsActiveTrue();

    Optional<Doctor> findByIdAndIsActiveTrue(Long id);

    List<Doctor> findBySpecialtyAndIsActiveTrue(Specialty specialty);

    @Query("SELECT d FROM Doctor d WHERE " +
            "LOWER(SUBSTRING(d.fullName, 1, LOCATE(' ', d.fullName) - 1)) LIKE LOWER(CONCAT('%', :lastName, '%')) " +
            "AND d.isActive = true")
    List<Doctor> findByLastNameContainingIgnoreCaseAndIsActiveTrue(@Param("lastName") String lastName);

    List<Doctor> findByFullNameContainingIgnoreCaseAndIsActiveTrue(String fullName);
}