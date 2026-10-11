package com.jerokysys.academic.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.academic.services.ExamResultService;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;
import com.jerokysys.commons.controller.BaseController;
import com.jerokysys.commons.domain.entity.ExamResult;
import com.jerokysys.dto.ExamResultDTO;

@RestController
@RequestMapping("/exam-results")
public class ExamResultController extends BaseController<ExamResult, ExamResultDTO, PagedFilter, ExamResultService> {

    private final ExamResultService examResultService;

    public ExamResultController(ExamResultService examResultService) {
        super(examResultService);
        this.examResultService = examResultService;
    }

    @GetMapping("/by-exam/{examId}")
    public Response<List<ExamResultDTO>> getByExam(
            @PathVariable Long examId) {
        return Response.success(examResultService.getByExam(examId));
    }

    @GetMapping("/by-student/{studentId}")
    public Response<List<ExamResultDTO>> getByStudent(
            @PathVariable Long studentId) {
        return Response.success(examResultService.getByStudent(studentId));
    }
}
