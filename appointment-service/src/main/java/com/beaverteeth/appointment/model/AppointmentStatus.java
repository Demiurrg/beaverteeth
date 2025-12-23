package com.beaverteeth.appointment.model;

public enum AppointmentStatus {
    PENDING("Ожидает подтверждения"),
    SCHEDULED("Запланирован"),
    CONFIRMED("Подтвержден"),
    REJECTED("Отклонен"),
    COMPLETED("Завершен"),
    CANCELLED("Отменен"),
    NO_SHOW("Пациент не явился");

    private final String displayName;

    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}