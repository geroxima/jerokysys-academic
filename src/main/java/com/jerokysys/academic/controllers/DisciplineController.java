package com.jerokysys.academic.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.academic.services.DisciplineService;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;
import com.jerokysys.commons.controller.BaseController;
import com.jerokysys.commons.domain.entity.Discipline;
import com.jerokysys.dto.DisciplineDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/disciplines")
public class DisciplineController extends BaseController<Discipline, DisciplineDTO, PagedFilter, DisciplineService> {

    private final DisciplineService disciplineService;

    public DisciplineController(DisciplineService disciplineService) {
        super(disciplineService);
        this.disciplineService = disciplineService;
    }

    @GetMapping("/by-name")
    public Response<List<DisciplineDTO>> getByNameIgnoreCase(
            @RequestParam String name) {
        return Response.success(disciplineService.getByNameIgnoreCase(name));
    }

    @GetMapping("/search-by-name")
    public Response<List<DisciplineDTO>> getByNameContainingIgnoreCase(
            @RequestParam String name) {
        return Response.success(disciplineService.getByNameContainingIgnoreCase(name));
    }

}