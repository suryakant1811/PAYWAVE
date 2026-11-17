package com.paywave.auth.repo;

import com.paywave.auth.domain.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID> {
}
