package com.beaverteeth.telegram.bot;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserSessionManager {

    private final Map<Long, UserSession> sessions = new HashMap<>();

    public UserSession getSession(Long chatId) {
        return sessions.computeIfAbsent(chatId, id ->
                UserSession.builder()
                        .chatId(id)
                        .state(BotState.START)
                        .data(new HashMap<>())
                        .build()
        );
    }

    public void updateSession(UserSession session) {
        sessions.put(session.getChatId(), session);
    }

    public void clearSession(Long chatId) {
        sessions.remove(chatId);
    }
}