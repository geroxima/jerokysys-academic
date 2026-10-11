package com.jerokysys.academic.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.Exam;
import com.jerokysys.dto.ExamDTO;

@Mapper(componentModel = "spring")
public interface ExamMapper {

    @Mapping(target = "danceClassId", source = "danceClass.id")
    ExamDTO toDto(Exam entity);

    List<ExamDTO> toDtoList(List<Exam> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "danceClass", ignore = true)
    @Mapping(target = "results", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Exam toEntity(ExamDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "danceClass", ignore = true)
    @Mapping(target = "results", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(ExamDTO dto, @MappingTarget Exam entity);
}