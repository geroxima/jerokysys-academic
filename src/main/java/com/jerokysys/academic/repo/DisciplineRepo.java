package com.jerokysys.academic.repo;

import java.util.List;

import com.jerokysys.commons.domain.entity.Discipline;
import com.jerokysys.commons.repository.BaseRepository;

public interface DisciplineRepo extends BaseRepository<Discipline, Long>{

    List<Discipline> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Discipline> findByNameContainingIgnoreCase(String fragment);

    
}