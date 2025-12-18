package com.beaverteeth.telegram.bot;

public enum BotState {
    START,
    WAITING_DOCTOR_LASTNAME,
    CHOOSING_DOCTOR,
    WAITING_DATE,
    WAITING_TIME_SLOT,
    WAITING_CONFIRMATION,
    VIEWING_SCHEDULE,
    MAIN_MENU,

    CHECKING_USER_REGISTRATION,  // проверяем зарегистрирован ли пользователь
    WAITING_FULL_NAME,           // ждем ФИО
    WAITING_AGE,                 // ждем возраст
    WAITING_ADDRESS,             // ждем адрес
    WAITING_PHONE,               // ждем телефон
    WAITING_EMAIL                // ждем email
}