package com.beaverteeth.patient.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ФИО не может быть пустым")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Min(value = 0, message = "Возраст не может быть отрицательным")
    @Max(value = 150, message = "Возраст не может быть больше 150")
    @Column(name = "age", nullable = false)
    private Integer age;

    @NotBlank(message = "Адрес не может быть пустым")
    @Column(name = "address", nullable = false)
    private String address;

    @NotBlank(message = "Телефон не может быть пустым")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный формат телефона")
    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Email(message = "Некорректный формат email")
    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "telegram_username", unique = true)
    private String telegramUsername;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}