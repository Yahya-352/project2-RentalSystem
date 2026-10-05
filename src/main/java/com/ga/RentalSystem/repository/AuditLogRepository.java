package com.ga.RentalSystem.repository;

import com.ga.RentalSystem.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog , Long> {

}
