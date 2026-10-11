package com.jerokysys.academic.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.ExamResult;
import com.jerokysys.dto.ExamResultDTO;

@Mapper(componentModel = "spring")
public interface ExamResultMapper {

    @Mapping(target = "examId", source = "exam.id")
    @Mapping(target = "studentId", source = "student.id")
    ExamResultDTO toDto(ExamResult entity);

    List<ExamResultDTO> toDtoList(List<ExamResult> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "exam", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ExamResult toEntity(ExamResultDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "exam", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(ExamResultDTO dto, @MappingTarget ExamResult entity);
}
