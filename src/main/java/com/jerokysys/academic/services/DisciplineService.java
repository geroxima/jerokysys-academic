package com.jerokysys.academic.services;

import com.jerokysys.academic.repo.DanceClassRepo;
import org.springframework.stereotype.Service;

import com.jerokysys.academic.mappers.DisciplineMapper;
import com.jerokysys.academic.repo.DisciplineRepo;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.domain.entity.Discipline;
import com.jerokysys.commons.exception.BusinessException;
import com.jerokysys.commons.service.AbstractBaseService;
import com.jerokysys.dto.DanceClassDTO;
import com.jerokysys.dto.DisciplineDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.Getter;

import java.util.List;

@Service

public class DisciplineService extends AbstractBaseService<Discipline, DisciplineDTO, PagedFilter> {

    @Getter
    private final DisciplineRepo disciplineRepo;
    private final DisciplineMapper mapper;

    public DisciplineService(DisciplineRepo disciplineRepo, DisciplineMapper mapper) {
        super(disciplineRepo);
        this.disciplineRepo = disciplineRepo;
        this.mapper = mapper;

    }

    @Override
    public DisciplineDTO create(DisciplineDTO dto) {
        Discipline entity = toEntity(dto);
        return toDto(disciplineRepo.save(entity));
    }

    @Override
    public DisciplineDTO update(Long id, DisciplineDTO dto) {
        Discipline entity = disciplineRepo.findById(id).orElseThrow(() -> new BusinessException(
                "ENTITY_NOT_FOUND",
                "Discipline not found with id: " + id));
        mapper.updateEntity(dto, entity);
        return toDto(disciplineRepo.save(entity));
    }

    public List<DisciplineDTO> getByNameIgnoreCase(String name) {
        return mapper.toDtoList(disciplineRepo.findByNameIgnoreCase(name));
    }

    public List<DisciplineDTO> getByNameContainingIgnoreCase(String name) {
        return mapper.toDtoList(disciplineRepo.findByNameContainingIgnoreCase(name));
    }

    @Override
    public PageResult<DisciplineDTO> search(PagedFilter filter) {
        Pageable pageable = buildPageable(filter);
        Page<Discipline> page = disciplineRepo.findAll(pageable);
        return toPageResult(toPageDto(page));

    }

    @Override
    public DisciplineDTO toDto(Discipline entity) {
        return mapper.toDto((entity));
    }

    @Override
    public Discipline toEntity(DisciplineDTO dto) {
        return mapper.toEntity(dto);
    }

}