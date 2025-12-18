package com.beaverteeth.telegram.config;

import com.beaverteeth.telegram.bot.ClinicTelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@Slf4j
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(ClinicTelegramBot clinicTelegramBot) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(clinicTelegramBot);
            log.info("✅ Телеграм бот успешно зарегистирирован!");
            log.info("Имя: {}", clinicTelegramBot.getBotUsername());
            return botsApi;
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка регистрации телеграм бота: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка регистрации телеграм бота", e);
        }
    }
}