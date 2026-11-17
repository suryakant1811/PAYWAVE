package com.paywave.user.repo;

import com.paywave.user.domain.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
}
