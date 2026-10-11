package com.jerokysys.academic.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jerokysys.academic.mappers.ExamResultMapper;
import com.jerokysys.academic.repo.ExamRepo;
import com.jerokysys.academic.repo.ExamResultRepo;
import com.jerokysys.academic.repo.StudentRepo;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.Exam;
import com.jerokysys.commons.domain.entity.ExamResult;
import com.jerokysys.commons.domain.entity.Student;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.ExamResultDTO;

import lombok.Getter;

@Service
public class ExamResultService extends AbstractBaseService<ExamResult, ExamResultDTO, PagedFilter> {

    @Getter
    private final ExamResultRepo examResultRepo;
    private final ExamRepo examRepo;
    private final StudentRepo studentRepo;
    private final ExamResultMapper mapper;

    public ExamResultService(ExamResultRepo examResultRepo,
            ExamRepo examRepo,
            StudentRepo studentRepo,
            ExamResultMapper mapper) {
        super(examResultRepo);
        this.examResultRepo = examResultRepo;
        this.examRepo = examRepo;
        this.studentRepo = studentRepo;
        this.mapper = mapper;
    }

    @Override
    public ExamResultDTO create(ExamResultDTO dto) {
        ExamResult entity = toEntity(dto);
        entity.setExam(loadExam(dto.getExamId()));
        entity.setStudent(loadStudent(dto.getStudentId()));
        assertUnique(dto.getExamId(), dto.getStudentId(), null);
        return toDto(examResultRepo.save(entity));
    }

    @Override
    public ExamResultDTO update(Long id, ExamResultDTO dto) {
        ExamResult entity = examResultRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "ExamResult not found with id: " + id));

        assertNoRepoint(dto, entity);
        mapper.updateEntity(dto, entity);
        assertUnique(entity.getExam().getId(), entity.getStudent().getId(), id);
        return toDto(examResultRepo.save(entity));
    }

    private void assertNoRepoint(ExamResultDTO dto, ExamResult entity) {
        if (dto.getExamId() != null && !entity.getExam().getId().equals(dto.getExamId())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "examId cannot change after creation. Only score and remarks can be updated.");
        }
        if (dto.getStudentId() != null && !entity.getStudent().getId().equals(dto.getStudentId())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "studentId cannot change after creation. Only score and remarks can be updated.");
        }
    }

    private void assertUnique(Long examId, Long studentId, Long excludeId) {
        if (!examResultRepo.existsByExamIdAndStudentId(examId, studentId)) {
            return;
        }
        List<ExamResult> same = examResultRepo.findByExamIdAndStudentId(examId, studentId);
        boolean conflict = same.stream()
                .anyMatch(r -> excludeId == null || !r.getId().equals(excludeId));
        if (conflict) {
            throw new BusinessException("CONFLICT",
                    "A result already exists for student " + studentId
                            + " in exam " + examId);
        }
    }

    @Override
    public PageResult<ExamResultDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<ExamResult> page = examResultRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public ExamResultDTO toDto(ExamResult entity) {
        return mapper.toDto(entity);
    }

    @Override
    public ExamResult toEntity(ExamResultDTO dto) {
        return mapper.toEntity(dto);
    }

    public List<ExamResultDTO> getByExam(Long examId) {
        return mapper.toDtoList(examResultRepo.findByExamId(examId));
    }

    public List<ExamResultDTO> getByStudent(Long studentId) {
        return mapper.toDtoList(examResultRepo.findByStudentId(studentId));
    }

    private Exam loadExam(Long examId) {
        if (examId == null) {
            throw new BusinessException("VALIDATION_ERROR", "examId is required");
        }
        return examRepo.findById(examId)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND", "Exam not found with id: " + examId));
    }

    private Student loadStudent(Long studentId) {
        if (studentId == null) {
            throw new BusinessException("VALIDATION_ERROR", "studentId is required");
        }
        return studentRepo.findById(studentId)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND", "Student not found with id: " + studentId));
    }
}
