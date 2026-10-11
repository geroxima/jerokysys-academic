package com.jerokysys.academic.mappers;

import java.time.LocalTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.Discipline;
import com.jerokysys.commons.domain.enums.ClassStatus;
import com.jerokysys.dto.DisciplineDTO;

@Mapper(componentModel = "spring")
public interface DisciplineMapper {

    DisciplineDTO toDto(Discipline entity);

    List<DisciplineDTO> toDtoList(List<Discipline> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Discipline toEntity(DisciplineDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(DisciplineDTO dto, @MappingTarget Discipline entity);

}