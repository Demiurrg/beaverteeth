package com.beaverteeth.telegram.controller;

import com.beaverteeth.telegram.bot.ClinicTelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final ClinicTelegramBot telegramBot;

    @PostMapping("/send")
    public void sendNotification(@RequestBody Map<String, Object> request) {
        try {
            Long chatId = ((Number) request.get("chatId")).longValue();
            String message = (String) request.get("message");

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId.toString());
            sendMessage.setText(message);
            sendMessage.setParseMode("Markdown");

            telegramBot.execute(sendMessage);

            log.info("Уведомление отправлено в chatId: {}", chatId);

        } catch (TelegramApiException e) {
            log.error("Ошибка отправки уведомления: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка отправки уведомления: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка обработки запроса: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ошибка обработки запроса: " + e.getMessage());
        }
    }
}