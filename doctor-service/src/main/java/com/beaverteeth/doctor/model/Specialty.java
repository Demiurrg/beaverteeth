package com.beaverteeth.doctor.model;

public enum Specialty {
    THERAPIST("Терапевт"),
    ORTHOPEDIST("Ортопед"),
    SURGEON("Хирург"),
    PEDIATRIC("Детский врач"),
    ORTHODONTIST("Ортодонт");

    private final String displayName;

    Specialty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}