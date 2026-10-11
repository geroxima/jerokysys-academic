package com.jerokysys.academic.repo;

import java.time.LocalDate;
import java.util.List;

import com.jerokysys.commons.domain.entity.Exam;
import com.jerokysys.commons.repository.BaseRepository;

public interface ExamRepo extends BaseRepository<Exam, Long> {

    List<Exam> findByDanceClassId(Long danceClassId);

    List<Exam> findByExamNameIgnoreCase(String examName);

    List<Exam> findByExamNameContainingIgnoreCase(String fragment);

    List<Exam> findByExamDate(LocalDate examDate);

    List<Exam> findByDanceClassIdAndExamDate(Long danceClassId, LocalDate examDate);

    boolean existsByDanceClassIdAndExamName(Long danceClassId, String examName);
}