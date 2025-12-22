package com.beaverteeth.doctor.repository;

import com.beaverteeth.doctor.model.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    List<Vacation> findByDoctorId(Long doctorId);

    @Query("SELECT v FROM Vacation v WHERE v.doctorId = :doctorId " +
            "AND (v.startDate <= :date AND v.endDate >= :date)")
    List<Vacation> findByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    @Query("SELECT v FROM Vacation v WHERE v.doctorId = :doctorId " +
            "AND (v.startDate <= :endDate AND v.endDate >= :startDate)")
    List<Vacation> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByDoctorIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long doctorId, LocalDate date, LocalDate sameDate);
}