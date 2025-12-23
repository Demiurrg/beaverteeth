package com.beaverteeth.telegram.bot;

import com.beaverteeth.telegram.service.AppointmentApiClient;
import com.beaverteeth.telegram.service.DoctorApiClient;
import com.beaverteeth.telegram.service.PatientApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class ClinicTelegramBot extends TelegramLongPollingBot {
    private final UserSessionManager sessionManager;
    private final AppointmentApiClient appointmentApiClient;
    private final DoctorApiClient doctorApiClient;
    private final PatientApiClient patientApiClient;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${doctor.service.url:http://localhost:8082")
    private String patientServiceUrl;

    private RestTemplate restTemplate;

    public ClinicTelegramBot(
            UserSessionManager sessionManager,
            AppointmentApiClient appointmentApiClient, DoctorApiClient doctorApiClient, PatientApiClient patientApiClient) {
        super(new DefaultBotOptions());
        this.sessionManager = sessionManager;
        this.appointmentApiClient = appointmentApiClient;
        this.doctorApiClient = doctorApiClient;
        this.patientApiClient = patientApiClient;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        log.info("=== БОТ ЗАРЕГИСТРИРОВАН ===");
        log.info("Username: {}", getBotUsername());
        log.info("Token: {}", getBotToken().substring(0, 10) + "...");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            // Получаем информацию о пользователе
            org.telegram.telegrambots.meta.api.objects.User user = update.getMessage().getFrom();
            String username = user.getUserName(); // ← это telegramUsername

            log.info("Получено сообщение: chatId={}, username={}, text={}", chatId, username, text);

            // Для команды /start проверяем регистрацию
            if (text.equals("/start")) {
                // Вызываем ТВОЙ метод handleStart с правильными параметрами
                handleStart(chatId, username);  // ← только chatId и username
            } else {
                handleMessage(chatId, text);
            }

        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(
                    update.getCallbackQuery().getMessage().getChatId(),
                    update.getCallbackQuery().getData()
            );
        }
    }

    private void handleMessage(Long chatId, String text) {
        UserSession session = sessionManager.getSession(chatId);

        switch (session.getState()) {
            case CHOOSING_DOCTOR:
                handleChoosingDoctor(chatId, session, text);
                break;

            case WAITING_FULL_NAME:
                handleFullName(chatId, session, text);
                break;

            case WAITING_AGE:
                handleAge(chatId, session, text);
                break;

            case WAITING_ADDRESS:
                handleAddress(chatId, session, text);
                break;

            case WAITING_PHONE:
                handlePhone(chatId, session, text);
                break;

            case WAITING_EMAIL:
                handleEmail(chatId, session, text);
                break;

            case WAITING_DOCTOR_LASTNAME:
                if (text.equalsIgnoreCase("👨‍⚕️ показать детали") ||
                        text.equalsIgnoreCase("показать детали") ||
                        text.equalsIgnoreCase("детали")) {
                    showDoctorsWithDetails(chatId);
                } else {
                    handleDoctorLastName(chatId, session, text);
                }
                break;

            case WAITING_DATE:
                handleDate(chatId, session, text);
                break;

            case MAIN_MENU:
                handleMainMenu(chatId, text);
                break;

            default:
                sendMessage(chatId, "Пожалуйста, используйте кнопки меню.");
                showMainMenu(chatId);
        }
    }

    private void handleCallbackQuery(Long chatId, String callbackData) {
        UserSession session = sessionManager.getSession(chatId);

        if (callbackData.startsWith("timeslot_")) {
            handleTimeSlotSelection(chatId, session, callbackData);
        } else if (callbackData.startsWith("date_")) {
            handleDateSelection(chatId, session, callbackData);
        } else if (callbackData.equals("confirm_yes")) {
            handleConfirmation(chatId, session, true);
        } else if (callbackData.equals("confirm_no")) {
            handleConfirmation(chatId, session, false);
        } else if (callbackData.equals("menu_book")) {
            startBookingProcess(chatId, session);
        } else if (callbackData.equals("menu_schedule")) {
            showScheduleMenu(chatId, session);
        } else if (callbackData.equals("menu_main")) {
            showMainMenu(chatId);
        } else if (callbackData.equals("show_doctor_details")) {
            showDoctorsWithDetails(chatId);
        }
    }

    private void showMainMenu(Long chatId) {
        UserSession session = sessionManager.getSession(chatId);
        boolean isRegistered = session.getData().containsKey("patientId");

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("👋 Добро пожаловать в стоматологическую клинику 'Элитные зубы'!\n\n" +
                "📋 Выберите действие:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Записаться на прием"
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton bookButton = new InlineKeyboardButton();
        bookButton.setText("📅 Записаться на прием");
        bookButton.setCallbackData("menu_book");
        row1.add(bookButton);

        // Кнопка "Наши врачи" ← ДОБАВЬТЕ ЭТУ КНОПКУ!
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton doctorsButton = new InlineKeyboardButton();
        doctorsButton.setText("👨‍⚕️ Наши врачи");
        doctorsButton.setCallbackData("show_doctor_details");
        row2.add(doctorsButton);

        // Кнопка "Расписание"
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton scheduleButton = new InlineKeyboardButton();
        scheduleButton.setText("📊 Посмотреть расписание врача");
        scheduleButton.setCallbackData("menu_schedule");
        row3.add(scheduleButton);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void startBookingProcess(Long chatId, UserSession session) {
        session.setState(BotState.WAITING_DOCTOR_LASTNAME);
        sessionManager.updateSession(session);

        showDoctorsBrief(chatId, session);
    }

    private void handleDoctorLastName(Long chatId, UserSession session, String lastName) {
        // Ищем врача по фамилии
        List<Map<String, Object>> doctors = doctorApiClient.searchDoctorsByLastName(lastName);

        if (doctors.isEmpty()) {
            sendMessage(chatId, "❌ Врач с фамилией \"" + lastName + "\" не найден.\n" +
                    "Пожалуйста, проверьте правильность ввода или нажмите *👨‍⚕️ Показать детали*");
            return;
        }

        // Если нашли несколько врачей с похожими фамилиями
        if (doctors.size() > 1) {
            StringBuilder message = new StringBuilder();
            message.append("🔍 Найдено несколько врачей:\n\n");

            for (int i = 0; i < doctors.size(); i++) {
                Map<String, Object> doctor = doctors.get(i);
                String fullName = (String) doctor.get("fullName");
                String specialty = (String) doctor.get("specialty");
                String specialtyDisplay = getSpecialtyDisplayName(specialty);

                message.append(i + 1).append(". *").append(fullName).append("*\n");
                message.append("   🎯 ").append(specialtyDisplay).append("\n\n");

                // Сохраняем mapping для быстрого выбора
                session.getData().put("doctor_" + i, doctor.get("id"));
            }

            message.append("👇 Введите *номер врача* или *полную фамилию*");

            session.setState(BotState.CHOOSING_DOCTOR);
            sessionManager.updateSession(session);

            sendMessage(chatId, message.toString());
            return;
        }

        // Если нашли одного врача
        Map<String, Object> doctor = doctors.get(0);
        Long doctorId = ((Number) doctor.get("id")).longValue();
        String fullName = (String) doctor.get("fullName");
        String specialty = (String) doctor.get("specialty");
        String specialtyDisplay = getSpecialtyDisplayName(specialty);

        session.setDoctorLastName(lastName);
        session.setSelectedDoctorId(doctorId);
        session.setState(BotState.WAITING_DATE);
        sessionManager.updateSession(session);

        sendMessage(chatId, "✅ Выбран врач: *" + fullName + "*\n" +
                "🎯 Специализация: " + specialtyDisplay + "\n\n" +
                "Теперь выберите дату для записи:");

        showDateSelection(chatId);
    }

    private void showDateSelection(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📅 Выберите дату для записи:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Генерируем даты на 7 дней вперед
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);

            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton dateButton = new InlineKeyboardButton();
            dateButton.setText(date.format(DateTimeFormatter.ofPattern("dd.MM (EE)", new Locale("ru"))));
            dateButton.setCallbackData("date_" + date.format(DateTimeFormatter.ISO_DATE));
            row.add(dateButton);

            rows.add(row);
        }

        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("menu_main");
        backRow.add(backButton);
        rows.add(backRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void handleDateSelection(Long chatId, UserSession session, String callbackData) {
        String dateStr = callbackData.replace("date_", "");
        LocalDate date = LocalDate.parse(dateStr);

        session.setAppointmentDate(date);
        session.setState(BotState.WAITING_TIME_SLOT);
        sessionManager.updateSession(session);

        showAvailableTimeSlots(chatId, session.getDoctorLastName(), date);
    }

    private void handleDate(Long chatId, UserSession session, String text) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate date = LocalDate.parse(text, formatter);

            session.setAppointmentDate(date);
            session.setState(BotState.WAITING_TIME_SLOT);
            sessionManager.updateSession(session);

            showAvailableTimeSlots(chatId, session.getDoctorLastName(), date);

        } catch (Exception e) {
            sendMessage(chatId, "❌ Неверный формат даты. Введите в формате ДД.ММ.ГГГГ");
        }
    }

    private void showAvailableTimeSlots(Long chatId, String doctorLastName, LocalDate date) {
        List<Map<String, Object>> timeSlots = appointmentApiClient
                .getAvailableTimeSlots(doctorLastName, date);

        if (timeSlots.isEmpty()) {
            sendMessage(chatId, "❌ На выбранную дату нет свободных слотов для врача " + doctorLastName);
            showMainMenu(chatId);
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🕐 Выберите удобное время для записи:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Map<String, Object> slot : timeSlots) {
            String startTimeStr = (String) slot.get("startTime");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            String displayTime = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton timeButton = new InlineKeyboardButton();
            timeButton.setText(displayTime);
            timeButton.setCallbackData("timeslot_" + startTimeStr);
            row.add(timeButton);

            rows.add(row);
        }

        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад к выбору даты");
        backButton.setCallbackData("menu_book");
        backRow.add(backButton);
        rows.add(backRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void handleTimeSlotSelection(Long chatId, UserSession session, String callbackData) {
        String timeStr = callbackData.replace("timeslot_", "");
        LocalDateTime selectedTime = LocalDateTime.parse(timeStr);

        session.setSelectedTime(selectedTime);
        session.setState(BotState.WAITING_CONFIRMATION); // ← сразу подтверждение!
        sessionManager.updateSession(session);

        showConfirmation(chatId, session); // ← сразу показываем подтверждение
    }

    private void showConfirmation(Long chatId, UserSession session) {
        // Получаем patientId безопасно
        Long patientId = getLongFromSession(session, "patientId");

        // Получаем ФИО пациента из сессии
        String patientFullName = session.getPatientFullName();

        // Если ФИО нет, пробуем получить из данных
        if (patientFullName == null) {
            patientFullName = (String) session.getData().get("patientFullName");
        }

        // Если всё ещё нет, используем "Вы"
        if (patientFullName == null || patientFullName.isEmpty()) {
            patientFullName = "Вы";
        }

        String messageText = String.format(
                "✅ Проверьте данные записи:\n\n" +
                        "👨‍⚕️ Врач: %s\n" +
                        "📅 Дата: %s\n" +
                        "🕐 Время: %s\n" +
                        "👤 Пациент: %s\n\n" +  // ← теперь показывает ФИО
                        "Всё верно?",
                session.getDoctorLastName(),
                session.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                session.getSelectedTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                patientFullName  // ← ФИО вместо "Вы"
        );

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(messageText);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("✅ Да, записаться");
        yesButton.setCallbackData("confirm_yes");

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("❌ Нет, изменить");
        noButton.setCallbackData("confirm_no");

        row.add(yesButton);
        row.add(noButton);
        rows.add(row);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void handleConfirmation(Long chatId, UserSession session, boolean confirmed) {
        if (confirmed) {
            // БЕЗОПАСНО получаем patientId
            Long patientId = getLongFromSession(session, "patientId");
            Long doctorId = getLongFromSession(session, "doctorId");

            String patientName = session.getPatientFullName();

            if (patientId == null || doctorId == null) {
                sendMessage(chatId, "❌ Ошибка: недостаточно данных для записи.");
                showMainMenu(chatId);
                return;
            }

            Map<String, Object> result = appointmentApiClient.createAppointment(
                    doctorId,
                    patientId,
                    session.getSelectedTime(),
                    "Запись через Telegram бота"
            );

            if (result != null && result.containsKey("error")) {
                sendMessage(chatId, "❌ Ошибка при создании записи: " + result.get("error"));
            } else if (result != null) {
                String successMessage = String.format(
                        "🎉 Запись успешно создана!\n\n" +
                                "👤 Пациент: %s\n" +
                                "👨‍⚕️ Врач: %s\n" +
                                "📅 Дата: %s\n" +
                                "🕐 Время: %s\n\n" +
                                "Ждем вас на прием!",
                        patientName != null ? patientName : "Вы",
                        session.getDoctorLastName(),
                        session.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        session.getSelectedTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                );
                sendMessage(chatId, successMessage);
            } else {
                sendMessage(chatId, "❌ Неизвестная ошибка при создании записи.");
            }
        } else {
            sendMessage(chatId, "Запись отменена.");
        }

        sessionManager.clearSession(chatId);
        showMainMenu(chatId);
    }

    private void handleMainMenu(Long chatId, String text) {
        if (text.equals("/start") || text.equals("Меню")) {
            showMainMenu(chatId);
        } else if (text.equals("Записаться на прием")) {
            UserSession session = sessionManager.getSession(chatId);
            startBookingProcess(chatId, session);
        } else {
            sendMessage(chatId, "Используйте кнопки меню или команды.");
            showMainMenu(chatId);
        }
    }

    private void showScheduleMenu(Long chatId, UserSession session) {
        sendMessage(chatId, "👨‍⚕️ Введите фамилию врача для просмотра расписания:");
        session.setState(BotState.VIEWING_SCHEDULE);
        sessionManager.updateSession(session);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private String getSpecialtyDisplayName(String specialtyCode) {
        if (specialtyCode == null) return "Не указана";

        switch (specialtyCode.toUpperCase()) {
            case "THERAPIST": return "Терапевт";
            case "ORTHOPEDIST": return "Ортопед";
            case "SURGEON": return "Хирург";
            case "PEDIATRIC": return "Детский врач";
            case "ORTHODONTIST": return "Ортодонт";
            default: return specialtyCode;
        }
    }

    private void showDoctorsWithDetails(Long chatId) {
        log.info("Показ детальной информации о врачах для чата: {}", chatId);

        // Получаем всех активных врачей через DoctorApiClient
        List<Map<String, Object>> doctors = doctorApiClient.getAllActiveDoctors();

        if (doctors.isEmpty()) {
            sendMessage(chatId, "❌ В данный момент нет доступных врачей.");
            showMainMenu(chatId);
            return;
        }

        // Формируем сообщение с детальной информацией
        StringBuilder message = new StringBuilder();
        message.append("👨‍⚕️ **Доступные врачи:**\n\n");

        for (int i = 0; i < doctors.size(); i++) {
            Map<String, Object> doctor = doctors.get(i);

            String fullName = (String) doctor.get("fullName");
            String specialty = (String) doctor.get("specialty");
            Integer totalExp = (Integer) doctor.get("totalExperience");
            Integer clinicExp = (Integer) doctor.get("clinicExperience");
            String education = (String) doctor.get("education");
            String certificates = (String) doctor.get("certificates");

            // Преобразуем код специализации в читаемый формат
            String specialtyDisplay = getSpecialtyDisplayName(specialty);

            message.append("━━━━━━━━━━━━━━━━━━━━\n");
            message.append("👤 **").append(fullName).append("**\n");
            message.append("🎯 **Специализация:** ").append(specialtyDisplay).append("\n");
            message.append("📊 **Общий стаж:** ").append(totalExp).append(" лет\n");
            message.append("🏥 **Стаж в клинике:** ").append(clinicExp).append(" лет\n");

            if (education != null && !education.isBlank()) {
                String shortEducation = education.length() > 100 ?
                        education.substring(0, 100) + "..." : education;
                message.append("🎓 **Образование:** ").append(shortEducation).append("\n");
            }

            if (certificates != null && !certificates.isBlank()) {
                String shortCertificates = certificates.length() > 100 ?
                        certificates.substring(0, 100) + "..." : certificates;
                message.append("📜 **Сертификаты:** ").append(shortCertificates).append("\n");
            }

            message.append("\n");
        }

        message.append("━━━━━━━━━━━━━━━━━━━━\n");
        message.append("Для записи нажмите **📅 Записаться на прием**");

        // Отправляем сообщение
        sendMessage(chatId, message.toString());
    }

    private void showDoctorsBrief(Long chatId, UserSession session) {
        log.info("Показ краткого списка врачей для чата: {}", chatId);

        // Получаем всех активных врачей
        List<Map<String, Object>> doctors = doctorApiClient.getAllActiveDoctors();

        if (doctors.isEmpty()) {
            sendMessage(chatId, "❌ В данный момент нет доступных врачей.");
            showMainMenu(chatId);
            return;
        }

        // Формируем сообщение с краткой информацией
        StringBuilder message = new StringBuilder();
        message.append("👨‍⚕️ **Выберите врача для записи:**\n\n");

        for (int i = 0; i < doctors.size(); i++) {
            Map<String, Object> doctor = doctors.get(i);

            String fullName = (String) doctor.get("fullName");
            String specialty = (String) doctor.get("specialty");
            Long doctorId = ((Number) doctor.get("id")).longValue();

            String specialtyDisplay = getSpecialtyDisplayName(specialty);

            message.append(i + 1).append(". **").append(fullName).append("**\n");
            message.append("   🎯 ").append(specialtyDisplay).append("\n\n");

            // Сохраняем mapping имени к ID для последующего выбора
            session.getData().put("doctor_" + fullName.toLowerCase(), doctorId);
        }

        message.append("👇 **Введите фамилию врача или выберите из списка выше**\n");
        message.append("Или нажмите **👨‍⚕️ Показать детали** для подробной информации");

        // Отправляем сообщение с inline-клавиатурой
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message.toString());
        sendMessage.setParseMode("Markdown");

        // Создаем inline-клавиатуру
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для показа детальной информации
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton detailsButton = new InlineKeyboardButton();
        detailsButton.setText("👨‍⚕️ Показать детали о врачах");
        detailsButton.setCallbackData("show_doctor_details");
        row1.add(detailsButton);

        // Кнопка назад
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в меню");
        backButton.setCallbackData("menu_main");
        row2.add(backButton);

        rows.add(row1);
        rows.add(row2);
        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void handleFullName(Long chatId, UserSession session, String fullName) {
        // Простая валидация ФИО
        if (fullName == null || fullName.trim().length() < 5) {
            sendMessage(chatId, "❌ ФИО слишком короткое. Введите полное ФИО:");
            return;
        }

        session.getData().put("fullName", fullName.trim());
        session.setState(BotState.WAITING_AGE);
        sessionManager.updateSession(session);

        sendMessage(chatId, "📝 Отлично! Теперь введите ваш возраст:");
    }

    private void handleAge(Long chatId, UserSession session, String ageText) {
        try {
            int age = Integer.parseInt(ageText);
            if (age < 0 || age > 150) {
                sendMessage(chatId, "❌ Возраст должен быть от 0 до 150 лет. Введите снова:");
                return;
            }

            session.getData().put("age", age);
            session.setState(BotState.WAITING_ADDRESS);
            sessionManager.updateSession(session);

            sendMessage(chatId, "🏠 Теперь введите ваш адрес:");

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Возраст должен быть числом. Введите снова:");
        }
    }

    private void handleAddress(Long chatId, UserSession session, String address) {
        if (address == null || address.trim().length() < 5) {
            sendMessage(chatId, "❌ Адрес слишком короткий. Введите полный адрес:");
            return;
        }

        session.getData().put("address", address.trim());
        session.setState(BotState.WAITING_PHONE);
        sessionManager.updateSession(session);

        sendMessage(chatId, "📱 Теперь введите ваш телефон (например: +79161234567):");
    }

    private void handlePhone(Long chatId, UserSession session, String phone) {
        // Проверяем формат телефона
        if (!phone.matches("^\\+?[0-9]{10,15}$")) {
            sendMessage(chatId, "❌ Неверный формат телефона. Введите в формате +79161234567:");
            return;
        }

        // Проверяем, не занят ли телефон
        Map<String, Object> existingPatient = patientApiClient.getPatientByPhone(phone);
        if (existingPatient != null) {
            sendMessage(chatId, "❌ Этот телефон уже зарегистрирован. Введите другой номер:");
            return;
        }

        session.getData().put("phone", phone);
        session.setState(BotState.WAITING_EMAIL);
        sessionManager.updateSession(session);

        sendMessage(chatId, "📧 Введите ваш email (если нет - напишите 'нет'):");
    }

    private void handleEmail(Long chatId, UserSession session, String email) {
        // Обработка "нет"
        if (email.equalsIgnoreCase("нет") || email.equalsIgnoreCase("no")) {
            email = null;
        }
        // Проверяем формат email если введен
        else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            sendMessage(chatId, "❌ Неверный формат email. Введите корректный email или 'нет':");
            return;
        }

        session.getData().put("email", email);

        // Все данные собраны - завершаем регистрацию
        completeRegistration(chatId, session);
    }

    private void handleChoosingDoctor(Long chatId, UserSession session, String text) {
        try {
            // Пробуем распознать номер врача
            if (text.matches("^\\d+$")) {
                int doctorNumber = Integer.parseInt(text) - 1; // Пользователь вводит 1,2,3...
                String key = "doctor_" + doctorNumber;

                if (session.getData().containsKey(key)) {
                    Long doctorId = (Long) session.getData().get(key);
                    // Получаем информацию о враче
                    Map<String, Object> doctor = doctorApiClient.getDoctorById(doctorId);

                    if (doctor != null) {
                        session.setSelectedDoctorId(doctorId);
                        session.setDoctorLastName((String) doctor.get("fullName"));
                        session.setState(BotState.WAITING_DATE);
                        sessionManager.updateSession(session);

                        sendMessage(chatId, "✅ Врач выбран! Теперь выберите дату:");
                        showDateSelection(chatId);
                        return;
                    }
                }
            }

            // Если не номер, то пробуем снова поиск по фамилии
            handleDoctorLastName(chatId, session, text);

        } catch (Exception e) {
            log.error("Ошибка при выборе врача: {}", e.getMessage());
            sendMessage(chatId, "❌ Ошибка выбора врача. Попробуйте снова.");
            showDoctorsBrief(chatId, session);
        }
    }

    private Long safeToLong(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }

        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("Не удалось преобразовать {} в Long: {}", obj, e.getMessage());
            return null;
        }
    }

    // Утилитный метод для получения Long из сессии
    private Long getLongFromSession(UserSession session, String key) {
        if (session == null || session.getData() == null) {
            return null;
        }
        return safeToLong(session.getData().get(key));
    }

    private void handleStart(Long chatId, String telegramUsername) {
        log.info("🟢 Обработка /start для chatId={}, username={}", chatId, telegramUsername);

        UserSession session = sessionManager.getSession(chatId);
        session.getData().put("telegramUsername", telegramUsername);

        if (telegramUsername == null) {
            sendMessage(chatId, "❌ У вас не установлен Telegram username.");
            return;
        }

        Map<String, Object> patient = patientApiClient.getPatientByTelegramUsername(telegramUsername);

        if (patient != null) {
            // Пользователь уже зарегистрирован - ОБНОВЛЯЕМ chatId
            Long patientId = ((Number) patient.get("id")).longValue();
            updatePatientChatId(patientId, chatId); // НОВЫЙ МЕТОД

            session.setState(BotState.MAIN_MENU);
            session.getData().put("patientId", patientId);
            session.getData().put("patientFullName", patient.get("fullName"));
            session.getData().put("patientChatId", chatId); // Сохраняем в сессии

            sessionManager.updateSession(session);

            sendMessage(chatId, "✅ Вы уже зарегистрированы! Добро пожаловать.");
            showMainMenu(chatId);
        } else {
            // Начинаем регистрацию
            session.setState(BotState.WAITING_FULL_NAME);
            session.getData().put("patientChatId", chatId); // Сохраняем chatId для будущего пациента
            sessionManager.updateSession(session);

            sendMessage(chatId, "👋 Добро пожаловать! Пройдем регистрацию...");
            sendMessage(chatId, "Введите ваше ФИО:");
        }
    }

    private void updatePatientChatId(Long patientId, Long chatId) {
        try {
            String url = patientServiceUrl + "/api/patients/" + patientId + "/chat-id";

            Map<String, Object> request = Map.of(
                    "telegramChatId", chatId,
                    "modifiedBy", "telegram_bot"
            );

            restTemplate.put(url, request);
            log.info("Обновлен telegramChatId для пациента {}: {}", patientId, chatId);

        } catch (Exception e) {
            log.warn("Не удалось обновить chatId для пациента {}: {}", patientId, e.getMessage());
        }
    }

    private void completeRegistration(Long chatId, UserSession session) {
        try {
            String telegramUsername = (String) session.getData().get("telegramUsername");
            Long patientChatId = (Long) session.getData().get("patientChatId");

            Map<String, Object> patientData = Map.of(
                    "fullName", session.getData().get("fullName"),
                    "age", session.getData().get("age"),
                    "address", session.getData().get("address"),
                    "phone", session.getData().get("phone"),
                    "email", session.getData().get("email"),
                    "telegramUsername", telegramUsername,
                    "telegramChatId", patientChatId, // ДОБАВЛЯЕМ CHAT ID
                    "notes", "Зарегистрирован через Telegram бот",
                    "createdBy", "telegram_bot"
            );

            Map<String, Object> createdPatient = patientApiClient.createPatient(patientData);

            if (createdPatient != null) {
                session.getData().put("patientId", createdPatient.get("id"));
                session.setState(BotState.MAIN_MENU);
                sessionManager.updateSession(session);

                sendMessage(chatId, "🎉 Регистрация успешно завершена!");
                showMainMenu(chatId);
            }

        } catch (Exception e) {
            log.error("Ошибка при регистрации: {}", e.getMessage());
            sendMessage(chatId, "❌ Произошла ошибка при регистрации.");
            session.setState(BotState.WAITING_FULL_NAME);
            sessionManager.updateSession(session);
        }
    }
}