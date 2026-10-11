package com.jerokysys.academic.mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.jerokysys.commons.domain.entity.Enrollment;
import com.jerokysys.dto.EnrollmentDTO;
import com.jerokysys.dto.EnrollmentStatus;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "classId", source = "danceClass.id")
    @Mapping(target = "status", source = "status", qualifiedByName = "toStatus")
    EnrollmentDTO toDto(Enrollment entity);

    List<EnrollmentDTO> toDtoList(List<Enrollment> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "danceClass", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "toStatusString")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Enrollment toEntity(EnrollmentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "danceClass", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "toStatusString")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntity(EnrollmentDTO dto, @MappingTarget Enrollment entity);

    @Named("toStatus")
    default EnrollmentStatus toStatus(String value) {
        return value == null ? null : EnrollmentStatus.fromValue(value);
    }

    @Named("toStatusString")
    default String toStatusString(EnrollmentStatus value) {
        return value == null ? null : value.getValue();
    }
}
