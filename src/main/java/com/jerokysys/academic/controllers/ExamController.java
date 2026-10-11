package com.jerokysys.academic.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.academic.services.ExamService;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;
import com.jerokysys.commons.controller.BaseController;
import com.jerokysys.commons.domain.entity.Exam;
import com.jerokysys.dto.ExamDTO;

@RestController
@RequestMapping("/exams")
public class ExamController extends BaseController<Exam, ExamDTO, PagedFilter, ExamService> {
    private final ExamService examService;

    public ExamController(ExamService examService) {
        super(examService);
        this.examService = examService;
    }

    @GetMapping("/by-class/{classId}")
    public Response<List<ExamDTO>> getByClass(
            @PathVariable Long classId) {
        return Response.success(examService.getByClass(classId));
    }

    @GetMapping("/by-name")
    public Response<List<ExamDTO>> getByName(
            @RequestParam String name) {
        return Response.success(examService.getByName(name));
    }

    @GetMapping("/search-by-name")
    public Response<List<ExamDTO>> searchByName(
            @RequestParam String name) {
        return Response.success(examService.searchByName(name));
    }

    @GetMapping("/by-date")
    public Response<List<ExamDTO>> getByDate(
            @RequestParam LocalDate date) {
        return Response.success(examService.getByDate(date));
    }

}