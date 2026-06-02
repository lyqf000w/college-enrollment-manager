package com.example.enrollment.controller;

import com.example.enrollment.entity.EnrollRecord;
import com.example.enrollment.service.EnrollmentService;
import com.example.enrollment.service.ImportResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<EnrollRecord> records = enrollmentService.resetToSampleRecords();
        fillModel(model, records, "", "已加载后台样例选课数据");
        model.addAttribute("csvText", "");
        return "index";
    }

    @PostMapping("/enrollments/import")
    public String importCsv(@RequestParam("csvText") String csvText, Model model) {
        ImportResult result = enrollmentService.importFromCsv(csvText);
        String message = "导入完成：输入 " + result.inputCount() + " 行，有效 "
                + result.validCount() + " 条，去重 " + result.duplicateCount() + " 条";
        fillModel(model, result.records(), "", message);
        model.addAttribute("csvText", csvText);
        model.addAttribute("errors", result.errors());
        return "index";
    }

    @GetMapping("/enrollments/search")
    public String search(@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                         Model model) {
        List<EnrollRecord> records = enrollmentService.search(keyword);
        String message = records.isEmpty() ? "无匹配选课记录" : "查询到 " + records.size() + " 条选课记录";
        fillModel(model, records, keyword, message);
        model.addAttribute("csvText", "");
        return "index";
    }

    private void fillModel(Model model, List<EnrollRecord> records, String keyword, String message) {
        model.addAttribute("records", records);
        model.addAttribute("groupedRecords", enrollmentService.groupByCourseType(records));
        model.addAttribute("keyword", keyword);
        model.addAttribute("message", message);
        model.addAttribute("errors", List.of());
    }
}
