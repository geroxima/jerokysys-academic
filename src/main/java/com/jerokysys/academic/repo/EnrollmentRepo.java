package com.jerokysys.academic.repo;

import com.jerokysys.commons.domain.entity.Enrollment;
import com.jerokysys.commons.repository.BaseRepository;
import java.util.List;
import java.time.LocalDate;

public interface EnrollmentRepo extends BaseRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByDanceClassId(Long danceClassId);

    List<Enrollment> findByStatus(String status);

    List<Enrollment> findByEnrollmentDate(LocalDate enrollmentDate);

}