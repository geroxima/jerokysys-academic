package com.jerokysys.academic.mappers;

import java.time.LocalTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.DanceClass;
import com.jerokysys.commons.domain.enums.ClassStatus;
import com.jerokysys.dto.DanceClassDTO;

@Mapper(componentModel = "spring")
public interface DanceClassMapper {

    @Mapping(target = "disciplineId", source = "discipline.id")
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "toTimeString")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "toTimeString")
    DanceClassDTO toDto(DanceClass entity);

    List<DanceClassDTO> toDtoList(List<DanceClass> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "discipline", ignore = true)
    @Mapping(target = "instructors", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "toLocalTime")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "toLocalTime")
    DanceClass toEntity(DanceClassDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "discipline", ignore = true)
    @Mapping(target = "instructors", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "toLocalTime")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "toLocalTime")
    void updateEntity(DanceClassDTO dto, @MappingTarget DanceClass entity);

    @Named("toTimeString")
    default String toTimeString(LocalTime value) {
        return value == null ? null : value.toString();
    }

    @Named("toLocalTime")
    default LocalTime toLocalTime(String value) {
        return value == null ? null : LocalTime.parse(value);
    }

    @AfterMapping
    default void defaultStatus(@MappingTarget DanceClass entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(ClassStatus.ACTIVE);
        }
    }
}
