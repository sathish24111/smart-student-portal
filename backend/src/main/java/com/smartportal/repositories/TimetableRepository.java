package com.smartportal.repositories;

import com.smartportal.models.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByStaffId(Long staffId);
    List<Timetable> findBySubjectDepartmentId(Long departmentId);
    List<Timetable> findBySubjectDepartmentIdAndDayOfWeek(Long departmentId, String dayOfWeek);
}
