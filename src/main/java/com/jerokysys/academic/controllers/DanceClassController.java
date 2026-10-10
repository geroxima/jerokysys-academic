package com.jerokysys.academic.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.academic.services.DanceClassService;
import com.jerokysys.commons.common.PageResult;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;
import com.jerokysys.commons.controller.BaseController;
import com.jerokysys.commons.domain.entity.DanceClass;
import com.jerokysys.dto.DanceClassDTO;

@RestController
@RequestMapping("/classes")
public class DanceClassController extends BaseController<DanceClass, DanceClassDTO, PagedFilter, DanceClassService> {
    private final DanceClassService danceClassService;

    public DanceClassController(DanceClassService danceClassService) {
        super(danceClassService);
        this.danceClassService = danceClassService;
    }

    @GetMapping("/by-discipline/{disciplineId}")
    public Response<List<DanceClassDTO>> getByDiscipline(
            @PathVariable Long disciplineId) {
        return Response.success(danceClassService.getByDiscipline(disciplineId));
    }

    @GetMapping("/by-status")
    public Response<List<DanceClassDTO>> getByStatus(
            @RequestParam String status) {
        return Response.success(danceClassService.getByStatus(status));
    }

}