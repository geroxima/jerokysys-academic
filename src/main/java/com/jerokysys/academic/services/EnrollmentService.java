package com.jerokysys.academic.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jerokysys.academic.mappers.EnrollmentMapper;
import com.jerokysys.academic.repo.DanceClassRepo;
import com.jerokysys.academic.repo.EnrollmentRepo;
import com.jerokysys.academic.repo.StudentRepo;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.DanceClass;
import com.jerokysys.commons.domain.entity.Enrollment;
import com.jerokysys.commons.domain.entity.Student;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.EnrollmentDTO;
import com.jerokysys.dto.EnrollmentStatus;

import lombok.Getter;

@Service
public class EnrollmentService extends AbstractBaseService<Enrollment, EnrollmentDTO, PagedFilter> {

    @Getter
    private final EnrollmentRepo enrollmentRepo;
    private final StudentRepo studentRepo;
    private final DanceClassRepo danceClassRepo;
    private final EnrollmentMapper mapper;

    public EnrollmentService(EnrollmentRepo enrollmentRepo,
            StudentRepo studentRepo,
            DanceClassRepo danceClassRepo,
            EnrollmentMapper mapper) {
        super(enrollmentRepo);
        this.enrollmentRepo = enrollmentRepo;
        this.studentRepo = studentRepo;
        this.danceClassRepo = danceClassRepo;
        this.mapper = mapper;
    }

    @Override
    public EnrollmentDTO create(EnrollmentDTO dto) {
        Enrollment entity = toEntity(dto);
        entity.setStudent(loadStudent(dto.getStudentId()));
        entity.setDanceClass(loadDanceClass(dto.getClassId()));
        if (entity.getEnrollmentDate() == null) {
            entity.setEnrollmentDate(LocalDate.now());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(EnrollmentStatus.ACTIVE.getValue());
        }
        return toDto(enrollmentRepo.save(entity));
    }

    @Override
    public EnrollmentDTO update(Long id, EnrollmentDTO dto) {
        Enrollment entity = enrollmentRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Enrollment not found with id: " + id));

        mapper.updateEntity(dto, entity);
        if (dto.getStudentId() != null) {
            entity.setStudent(loadStudent(dto.getStudentId()));
        }
        if (dto.getClassId() != null) {
            entity.setDanceClass(loadDanceClass(dto.getClassId()));
        }
        return toDto(enrollmentRepo.save(entity));
    }

    @Override
    public PageResult<EnrollmentDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<Enrollment> page = enrollmentRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public EnrollmentDTO toDto(Enrollment entity) {
        return mapper.toDto(entity);
    }

    public List<EnrollmentDTO> getByStudent(Long studentId) {
        return mapper.toDtoList(enrollmentRepo.findByStudentId(studentId));
    }

    public List<EnrollmentDTO> getByClass(Long classId) {
        return mapper.toDtoList(enrollmentRepo.findByDanceClassId(classId));
    }

    public List<EnrollmentDTO> getByStatus(String status) {
        final EnrollmentStatus enrollmentStatus;
        try {
            enrollmentStatus = EnrollmentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid status: " + status);
        }
        return mapper.toDtoList(enrollmentRepo.findByStatus(enrollmentStatus.getValue()));
    }

    public List<EnrollmentDTO> getByDate(LocalDate date) {
        return mapper.toDtoList(enrollmentRepo.findByEnrollmentDate(date));
    }

    @Override
    public Enrollment toEntity(EnrollmentDTO dto) {
        return mapper.toEntity(dto);
    }

    private Student loadStudent(Long studentId) {
        if (studentId == null) {
            throw new BusinessException("VALIDATION_ERROR", "studentId is required");
        }
        return studentRepo.findById(studentId)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND", "Student not found with id: " + studentId));
    }

    private DanceClass loadDanceClass(Long classId) {
        if (classId == null) {
            throw new BusinessException("VALIDATION_ERROR", "classId is required");
        }
        return danceClassRepo.findById(classId)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND", "DanceClass not found with id: " + classId));
    }
}
