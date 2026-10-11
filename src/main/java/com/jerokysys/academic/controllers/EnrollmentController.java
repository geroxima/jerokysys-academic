package com.jerokysys.academic.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jerokysys.academic.services.EnrollmentService;
import com.jerokysys.commons.common.PagedFilter;
import com.jerokysys.commons.common.Response;
import com.jerokysys.commons.controller.BaseController;
import com.jerokysys.commons.domain.entity.Enrollment;
import com.jerokysys.dto.EnrollmentDTO;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController extends BaseController<Enrollment, EnrollmentDTO, PagedFilter, EnrollmentService> {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        super(enrollmentService);
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/by-student/{studentId}")
    public Response<List<EnrollmentDTO>> getByStudent(
            @PathVariable Long studentId) {
        return Response.success(enrollmentService.getByStudent(studentId));
    }

    @GetMapping("/by-class/{classId}")
    public Response<List<EnrollmentDTO>> getByClass(
            @PathVariable Long classId) {
        return Response.success(enrollmentService.getByClass(classId));
    }

    @GetMapping("/by-status")
    public Response<List<EnrollmentDTO>> getByStatus(
            @RequestParam String status) {
        return Response.success(enrollmentService.getByStatus(status));
    }

    @GetMapping("/by-date")
    public Response<List<EnrollmentDTO>> getByDate(
            @RequestParam LocalDate date) {
        return Response.success(enrollmentService.getByDate(date));
    }
}
