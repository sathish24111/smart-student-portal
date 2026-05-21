package com.smartportal.repositories;

import com.smartportal.models.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByStaffId(Long staffId);
    List<Assignment> findBySubjectDepartmentId(Long departmentId);
    List<Assignment> findBySubjectId(Long subjectId);
}
