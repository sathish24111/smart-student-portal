package com.smartportal.repositories;

import com.smartportal.models.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {
    List<FeeRecord> findByStudentId(Long studentId);
    Optional<FeeRecord> findByStudentIdAndStatus(Long studentId, String status);
}
