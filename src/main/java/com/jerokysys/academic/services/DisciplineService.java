package com.jerokysys.academic.services;

import com.jerokysys.academic.config.DomainCache;
import com.jerokysys.academic.config.RedisCacheConfig;
import com.jerokysys.academic.repo.DanceClassRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
import org.springframework.security.access.prepost.PreAuthorize;

@Service

@PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','SECRETARY')")
public class DisciplineService extends AbstractBaseService<Discipline, DisciplineDTO, PagedFilter> {

    @Getter
    private final DisciplineRepo disciplineRepo;
    private final DisciplineMapper mapper;
    private final DomainCache domainCache;

    public DisciplineService(DisciplineRepo disciplineRepo, DisciplineMapper mapper, DomainCache domainCache) {
        super(disciplineRepo);
        this.disciplineRepo = disciplineRepo;
        this.mapper = mapper;
        this.domainCache = domainCache;

    }

    @Override
    @Cacheable(value = RedisCacheConfig.CACHE_DISCIPLINE, key = "#id")
    public DisciplineDTO getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<DisciplineDTO> getAll() {
        List<DisciplineDTO> list = super.getAll();
        domainCache.putAll(RedisCacheConfig.CACHE_DISCIPLINE, list, DisciplineDTO::getId);
        return list;
    }

    @Override
    @CachePut(value = RedisCacheConfig.CACHE_DISCIPLINE,
            key = "#result.id",
            unless = "#result == null || #result.id == null")
    @CacheEvict(value = RedisCacheConfig.CACHE_DISCIPLINE, allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public DisciplineDTO create(DisciplineDTO dto) {
        Discipline entity = toEntity(dto);
        return toDto(disciplineRepo.save(entity));
    }

    @Override
    @CacheEvict(value = RedisCacheConfig.CACHE_DISCIPLINE, allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public DisciplineDTO update(Long id, DisciplineDTO dto) {
        Discipline entity = disciplineRepo.findById(id).orElseThrow(() -> new BusinessException(
                "ENTITY_NOT_FOUND",
                "Discipline not found with id: " + id));
        mapper.updateEntity(dto, entity);
        return toDto(disciplineRepo.save(entity));
    }

    @Override
    @CacheEvict(value = RedisCacheConfig.CACHE_DISCIPLINE, allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public void delete(Long id) {
        super.delete(id);
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