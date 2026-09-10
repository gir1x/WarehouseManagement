package com.example.wms.repository;

import com.example.wms.domain.LoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, UUID> {
}
