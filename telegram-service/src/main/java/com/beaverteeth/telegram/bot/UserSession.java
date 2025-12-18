package com.beaverteeth.telegram.bot;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {
    private Long chatId;
    private BotState state;
    private Map<String, Object> data;

    // Геттеры для удобства
    public String getDoctorLastName() {
        return data != null ? (String) data.get("doctorLastName") : null;
    }

    public void setDoctorLastName(String lastName) {
        if (data != null) {
            data.put("doctorLastName", lastName);
        }
    }

    public LocalDate getAppointmentDate() {
        return data != null ? (LocalDate) data.get("appointmentDate") : null;
    }

    public void setAppointmentDate(LocalDate date) {
        if (data != null) {
            data.put("appointmentDate", date);
        }
    }

    public LocalDateTime getSelectedTime() {
        return data != null ? (LocalDateTime) data.get("selectedTime") : null;
    }

    public void setSelectedTime(LocalDateTime time) {
        if (data != null) {
            data.put("selectedTime", time);
        }
    }

    public String getPatientPhone() {
        return data != null ? (String) data.get("patientPhone") : null;
    }

    public void setPatientPhone(String phone) {
        if (data != null) {
            data.put("patientPhone", phone);
        }
    }

    public Long getSelectedDoctorId() {
        return data != null ? (Long) data.get("doctorId") : null;
    }

    public void setSelectedDoctorId(Long doctorId) {
        if (data != null) {
            data.put("doctorId", doctorId);
        }
    }

    public String getPatientFullName() {
        return data != null ? (String) data.get("patientFullName") : null;
    }

    public void setPatientFullName(String fullName) {
        if (data != null) {
            data.put("patientFullName", fullName);
        }
    }
}