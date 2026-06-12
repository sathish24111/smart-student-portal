package com.smartportal.services;

import com.smartportal.models.AuditLog;
import com.smartportal.repositories.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logAction(String roleType, String username, String action, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setRoleType(roleType);
        log.setUsername(username);
        log.setAction(action);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }
}
