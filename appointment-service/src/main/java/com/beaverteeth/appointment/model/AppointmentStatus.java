package com.beaverteeth.appointment.model;

public enum AppointmentStatus {
    SCHEDULED("Запланирован"),
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