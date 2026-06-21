package com.relic.platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
