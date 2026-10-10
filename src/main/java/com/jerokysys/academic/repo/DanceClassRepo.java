package com.jerokysys.academic.repo;

import java.time.LocalTime;
import java.util.List;
import com.jerokysys.commons.domain.entity.DanceClass;
import com.jerokysys.commons.domain.enums.ClassStatus;
import com.jerokysys.commons.repository.BaseRepository;

public interface DanceClassRepo extends BaseRepository<DanceClass, Long> {

    List<DanceClass> findByLevel(String level);

    List<DanceClass> findByDisciplineId(Long disciplineId);

    List<DanceClass> findByStatus(ClassStatus status);

    List<DanceClass> findByDayOfWeek(String dayOfWeek);

    List<DanceClass> findByDisciplineIdAndLevel(Long disciplineId, String level);

    List<DanceClass> findByDisciplineIdAndStatus(Long disciplineId, ClassStatus status);

    boolean existsByDisciplineIdAndLevelAndDayOfWeekAndStartTime(
            Long disciplineId, String level, String dayOfWeek, LocalTime startTime);

    List<DanceClass> findByInstructorsId(Long instructorId);
    
    
}


