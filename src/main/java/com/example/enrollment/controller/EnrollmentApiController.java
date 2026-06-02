package com.example.enrollment.controller;

import com.example.enrollment.entity.EnrollRecord;
import com.example.enrollment.service.EnrollmentService;
import com.example.enrollment.service.ImportResult;
import com.example.enrollment.service.PagedResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnrollmentApiController {

    private final EnrollmentService enrollmentService;

    public EnrollmentApiController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/api/enrollments")
    public PagedResult<EnrollRecord> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                          @RequestParam(value = "size", defaultValue = "20") int size) {
        return enrollmentService.getAllRecords(page, size);
    }

    @PostMapping("/api/enrollments/import")
    public ImportResult importCsv(@RequestParam("csvText") String csvText) {
        return enrollmentService.importFromCsv(csvText);
    }

    @GetMapping("/api/enrollments/search")
    public PagedResult<EnrollRecord> search(@RequestParam(value = "keyword", defaultValue = "") String keyword,
                                            @RequestParam(value = "page", defaultValue = "1") int page,
                                            @RequestParam(value = "size", defaultValue = "20") int size) {
        return enrollmentService.search(keyword, page, size);
    }

    @PostMapping("/api/enrollments/clear")
    public PagedResult<EnrollRecord> clear() {
        enrollmentService.clearRecords();
        return enrollmentService.getAllRecords(1, 20);
    }
}
