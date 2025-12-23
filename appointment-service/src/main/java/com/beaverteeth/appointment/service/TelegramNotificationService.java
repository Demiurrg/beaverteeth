package com.beaverteeth.appointment.service;

import com.beaverteeth.appointment.model.Appointment;
import com.beaverteeth.appointment.model.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final RestTemplate restTemplate;
    private final TimeSlotService timeSlotService;

    @Value("${telegram.bot.url:http://localhost:8085}")
    private String telegramBotUrl;

    public void sendAppointmentConfirmation(Appointment appointment,
                                            AppointmentStatus oldStatus,
                                            AppointmentStatus newStatus) {

        Long chatId = appointment.getPatientChatId();

        if (chatId == null) {
            log.warn("Не удалось отправить уведомление: у пациента {} нет chatId",
                    appointment.getPatientId());
            return;
        }

        try {
            String doctorName = timeSlotService.getDoctorName(appointment.getDoctorId());
            String patientName = timeSlotService.getPatientName(appointment.getPatientId());

            String message;
            if (newStatus == AppointmentStatus.CONFIRMED) {
                message = createConfirmedMessage(appointment, doctorName, patientName);
            } else if (newStatus == AppointmentStatus.REJECTED) {
                message = createRejectedMessage(appointment, doctorName, patientName);
            } else {
                return;
            }

            // Отправляем запрос к Telegram боту
            String url = telegramBotUrl + "/api/notifications/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                    "chatId", chatId,
                    "message", message
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(url, entity, String.class);

            log.info("Уведомление отправлено пациенту {} (chatId: {})", patientName, chatId);

        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления: {}", e.getMessage());
        }
    }

    private String createConfirmedMessage(Appointment appointment, String doctorName, String patientName) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return String.format(
                "✅ *Ваша запись подтверждена!*\n\n" +
                        "👤 Пациент: %s\n" +
                        "👨‍⚕️ Врач: %s\n" +
                        "📅 Дата: %s\n" +
                        "🕐 Время: %s - %s\n\n" +
                        "Ждем вас на прием! Пожалуйста, приходите за 10 минут до назначенного времени.",
                patientName,
                doctorName,
                appointment.getStartTime().format(dateFormatter),
                appointment.getStartTime().format(timeFormatter),
                appointment.getEndTime().format(timeFormatter)
        );
    }

    private String createRejectedMessage(Appointment appointment, String doctorName, String patientName) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String reason = appointment.getConfirmationNotes() != null ?
                "\nПричина: " + appointment.getConfirmationNotes() : "";

        return String.format(
                "❌ *Ваша запись отклонена*\n\n" +
                        "👤 Пациент: %s\n" +
                        "👨‍⚕️ Врач: %s\n" +
                        "📅 Дата: %s\n" +
                        "🕐 Время: %s - %s\n%s\n\n" +
                        "Пожалуйста, выберите другое время для записи.",
                patientName,
                doctorName,
                appointment.getStartTime().format(dateFormatter),
                appointment.getStartTime().format(timeFormatter),
                appointment.getEndTime().format(timeFormatter),
                reason
        );
    }
}