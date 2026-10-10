package com.jerokysys.academic.services;

import org.springframework.stereotype.Service;

import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.DanceClassDTO;

import lombok.Getter;

import com.jerokysys.academic.mappers.DanceClassMapper;
import com.jerokysys.academic.repo.DanceClassRepo;
import com.jerokysys.academic.repo.DisciplineRepo;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.DanceClass;
import com.jerokysys.commons.domain.entity.Discipline;
import com.jerokysys.commons.exception.BusinessException;
import java.util.List;
import com.jerokysys.commons.domain.enums.ClassStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.Getter;

@Service
public class DanceClassService extends AbstractBaseService<DanceClass, DanceClassDTO, PagedFilter> {

    @Getter
    private final DanceClassRepo danceClassRepo;
    private final DisciplineRepo disciplineRepo;
    private final DanceClassMapper mapper;

    public DanceClassService(DanceClassRepo danceClassRepo,
            DisciplineRepo disciplineRepo,
            DanceClassMapper mapper) {
        super(danceClassRepo);
        this.danceClassRepo = danceClassRepo;
        this.disciplineRepo = disciplineRepo;
        this.mapper = mapper;
    }

    @Override
    public DanceClassDTO create(DanceClassDTO dto) {
        DanceClass entity = toEntity(dto);
        entity.setDiscipline(loadDiscipline(dto.getDisciplineId()));
        return toDto(danceClassRepo.save(entity));
    }

    @Override
    public DanceClassDTO update(Long id, DanceClassDTO dto) {
        DanceClass entity = danceClassRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND",
                        "DanceClass not found with id: " + id));

        mapper.updateEntity(dto, entity);
        if (dto.getDisciplineId() != null) {
            entity.setDiscipline(loadDiscipline(dto.getDisciplineId()));
        }
        return toDto(danceClassRepo.save(entity));
    }

    public List<DanceClassDTO> getByDiscipline(Long disciplineId) {
        return mapper.toDtoList(danceClassRepo.findByDisciplineId(disciplineId));
    }

    public List<DanceClassDTO> getByStatus(String status) {
        final ClassStatus classStatus;
        try {
            classStatus = ClassStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid status: " + status);
        }
        return mapper.toDtoList(danceClassRepo.findByStatus(classStatus));
    }

    @Override
    public PageResult<DanceClassDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<DanceClass> page = danceClassRepo.findAll(pageable);
        return toPageResult(toPageDto(page));
    }

    @Override
    public DanceClassDTO toDto(DanceClass entity) {
        return mapper.toDto(entity);
    }

    @Override
    public DanceClass toEntity(DanceClassDTO dto) {
        return mapper.toEntity(dto);
    }

    private Discipline loadDiscipline(Long disciplineId) {
        if (disciplineId == null) {
            throw new BusinessException("VALIDATION_ERROR", "disciplineId is required");
        }
        return disciplineRepo.findById(disciplineId)
                .orElseThrow(() -> new BusinessException(
                        "ENTITY_NOT_FOUND", "Discipline not found with id: " + disciplineId));
    }
}