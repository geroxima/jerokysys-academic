package com.jerokysys.academic.repo;

import java.util.List;

import com.jerokysys.commons.domain.entity.ExamResult;
import com.jerokysys.commons.repository.BaseRepository;

public interface ExamResultRepo extends BaseRepository<ExamResult, Long> {

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByStudentId(Long studentId);

    List<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);
}
