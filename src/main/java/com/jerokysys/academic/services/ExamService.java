package com.jerokysys.academic.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.jerokysys.academic.config.DomainCache;
import com.jerokysys.academic.config.RedisCacheConfig;
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
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','SECRETARY')")
public class ExamService extends AbstractBaseService<Exam, ExamDTO, PagedFilter> {

    @Getter
    private final ExamRepo examRepo;
    private final DanceClassRepo danceClassRepo;
    private final ExamMapper mapper;
    private final DomainCache domainCache;

    public ExamService(ExamRepo examRepo,
            DanceClassRepo danceClassRepo,
            ExamMapper mapper,
            DomainCache domainCache) {
        super(examRepo);
        this.examRepo = examRepo;
        this.danceClassRepo = danceClassRepo;
        this.mapper = mapper;
        this.domainCache = domainCache;
    }

    @Override
    @Cacheable(value = RedisCacheConfig.CACHE_EXAM, key = "#id")
    public ExamDTO getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<ExamDTO> getAll() {
        List<ExamDTO> list = super.getAll();
        domainCache.putAll(RedisCacheConfig.CACHE_EXAM, list, ExamDTO::getId);
        return list;
    }

    @Override
    @CachePut(value = RedisCacheConfig.CACHE_EXAM,
            key = "#result.id",
            unless = "#result == null || #result.id == null")
    @CacheEvict(value = RedisCacheConfig.CACHE_EXAM, allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public ExamDTO create(ExamDTO dto) {
        Exam entity = toEntity(dto);
        entity.setDanceClass(loadDanceClass(dto.getDanceClassId()));
        return toDto(examRepo.save(entity));
    }

    @Override
    @CacheEvict(value = RedisCacheConfig.CACHE_EXAM, allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
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
    @CacheEvict(value = RedisCacheConfig.CACHE_EXAM, allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
    public void delete(Long id) {
        super.delete(id);
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

    @Cacheable(value = RedisCacheConfig.CACHE_EXAM, key = "'class:' + #classId")
    public List<ExamDTO> getByClass(Long classId) {
        return mapper.toDtoList(examRepo.findByDanceClassId(classId));
    }

    @Cacheable(value = RedisCacheConfig.CACHE_EXAM, key = "'name:' + #name")
    public List<ExamDTO> getByName(String name) {
        return mapper.toDtoList(examRepo.findByExamNameIgnoreCase(name));
    }

    @Cacheable(value = RedisCacheConfig.CACHE_EXAM, key = "'search:' + #fragment")
    public List<ExamDTO> searchByName(String fragment) {
        return mapper.toDtoList(examRepo.findByExamNameContainingIgnoreCase(fragment));
    }

    @Cacheable(value = RedisCacheConfig.CACHE_EXAM, key = "'date:' + #date")
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