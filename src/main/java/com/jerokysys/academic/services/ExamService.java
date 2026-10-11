package com.jerokysys.academic.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jerokysys.academic.mappers.ExamMapper;
import com.jerokysys.academic.repo.DanceClassRepo;
import com.jerokysys.academic.repo.ExamRepo;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.DanceClass;
import com.jerokysys.commons.domain.entity.Exam;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.ExamDTO;

import lombok.Getter;

@Service
public class ExamService extends AbstractBaseService<Exam, ExamDTO, PagedFilter> {

    @Getter
    private final ExamRepo examRepo;
    private final DanceClassRepo danceClassRepo;
    private final ExamMapper mapper;

    public ExamService(ExamRepo examRepo,
            DanceClassRepo danceClassRepo,
            ExamMapper mapper) {
        super(examRepo);
        this.examRepo = examRepo;
        this.danceClassRepo = danceClassRepo;
        this.mapper = mapper;
    }

    @Override
    public ExamDTO create(ExamDTO dto) {
        Exam entity = toEntity(dto);
        entity.setDanceClass(loadDanceClass(dto.getDanceClassId()));
        return toDto(examRepo.save(entity));
    }

    @Override
    public ExamDTO update(Long id, ExamDTO dto) {
        Exam entity = examRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "Exam not found with id: " + id));

        mapper.updateEntity(dto, entity);
        if (dto.getDanceClassId() != null) {
            entity.setDanceClass(loadDanceClass(dto.getDanceClassId()));
        }
        return toDto(examRepo.save(entity));
    }

    @Override
    public PageResult<ExamDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<Exam> page = examRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public ExamDTO toDto(Exam entity) {
        return mapper.toDto(entity);
    }

    @Override
    public Exam toEntity(ExamDTO dto) {
        return mapper.toEntity(dto);
    }

    public List<ExamDTO> getByClass(Long classId) {
        return mapper.toDtoList(examRepo.findByDanceClassId(classId));
    }

    public List<ExamDTO> getByName(String name) {
        return mapper.toDtoList(examRepo.findByExamNameIgnoreCase(name));
    }

    public List<ExamDTO> searchByName(String fragment) {
        return mapper.toDtoList(examRepo.findByExamNameContainingIgnoreCase(fragment));
    }

    public List<ExamDTO> getByDate(LocalDate date) {
        return mapper.toDtoList(examRepo.findByExamDate(date));
    }

    private DanceClass loadDanceClass(Long classId) {
        if (classId == null) {
            throw new BusinessException("VALIDATION_ERROR", "danceClassId is required");
        }
        return danceClassRepo.findById(classId)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND", "DanceClass not found with id: " + classId));
    }
}