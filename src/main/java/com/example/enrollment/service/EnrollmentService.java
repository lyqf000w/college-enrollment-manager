package com.example.enrollment.service;

import com.example.enrollment.entity.EnrollRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final List<String> VALID_COURSE_TYPES = List.of("公共课", "专业课", "选修课");
    private static final Comparator<EnrollRecord> RECORD_COMPARATOR = Comparator
            .comparing(EnrollRecord::getStudentId)
            .thenComparing(EnrollRecord::getCourseId);

    private final List<EnrollRecord> records = new CopyOnWriteArrayList<>();

    public EnrollmentService() {
        resetToSampleRecords();
    }

    public List<EnrollRecord> resetToSampleRecords() {
        records.clear();
        records.addAll(processRecords(sampleRecords()));
        return getAllRecords();
    }

    public List<EnrollRecord> getAllRecords() {
        return sortedCopy(records);
    }

    public ImportResult importFromCsv(String csvText) {
        List<EnrollRecord> parsedRecords = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String[] lines = Objects.toString(csvText, "").split("\\R");
        int inputCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            inputCount++;
            String[] fields = line.split(",", -1);
            if (fields.length < 3 || fields.length > 4) {
                errors.add("第 " + (i + 1) + " 行格式错误，应为：学生ID,课程ID,课程名称,课程类型");
                continue;
            }

            String studentId = fields[0].trim();
            String courseId = fields[1].trim();
            String courseName = fields[2].trim();
            String courseType = fields.length == 4 ? fields[3].trim() : "";

            if (studentId.isEmpty() || courseId.isEmpty() || courseName.isEmpty()) {
                errors.add("第 " + (i + 1) + " 行存在必填字段为空");
                continue;
            }

            parsedRecords.add(new EnrollRecord(studentId, courseId, courseName, normalizeCourseType(courseName, courseType)));
        }

        List<EnrollRecord> processedRecords = processRecords(parsedRecords);
        records.clear();
        records.addAll(processedRecords);

        int duplicateCount = Math.max(0, parsedRecords.size() - processedRecords.size());
        return new ImportResult(getAllRecords(), inputCount, parsedRecords.size(), duplicateCount, errors);
    }

    public List<EnrollRecord> search(String keyword) {
        String normalizedKeyword = Objects.toString(keyword, "").trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return getAllRecords();
        }

        return records.stream()
                .filter(record -> containsIgnoreCase(record.getStudentId(), normalizedKeyword)
                        || containsIgnoreCase(record.getCourseId(), normalizedKeyword)
                        || containsIgnoreCase(record.getCourseName(), normalizedKeyword)
                        || containsIgnoreCase(record.getCourseType(), normalizedKeyword))
                .sorted(RECORD_COMPARATOR)
                .collect(Collectors.toList());
    }

    public Map<String, List<EnrollRecord>> groupByCourseType(List<EnrollRecord> sourceRecords) {
        Map<String, List<EnrollRecord>> grouped = new LinkedHashMap<>();
        for (String courseType : VALID_COURSE_TYPES) {
            grouped.put(courseType, new ArrayList<>());
        }
        for (EnrollRecord record : sortedCopy(sourceRecords)) {
            grouped.computeIfAbsent(record.getCourseType(), key -> new ArrayList<>()).add(record);
        }
        return grouped;
    }

    private List<EnrollRecord> processRecords(List<EnrollRecord> sourceRecords) {
        Map<String, EnrollRecord> uniqueRecords = new LinkedHashMap<>();
        for (EnrollRecord record : sourceRecords) {
            EnrollRecord normalizedRecord = new EnrollRecord(
                    trim(record.getStudentId()),
                    trim(record.getCourseId()),
                    trim(record.getCourseName()),
                    normalizeCourseType(record.getCourseName(), record.getCourseType())
            );
            String key = normalizedRecord.getStudentId() + "#" + normalizedRecord.getCourseId();
            uniqueRecords.putIfAbsent(key, normalizedRecord);
        }

        List<EnrollRecord> result = new ArrayList<>(uniqueRecords.values());
        result.sort(RECORD_COMPARATOR);
        result.forEach(System.out::println);
        return result;
    }

    private List<EnrollRecord> sampleRecords() {
        return List.of(
                new EnrollRecord("S000001", "C000001", "Java程序设计", "专业课"),
                new EnrollRecord("S000002", "C000003", "计算机网络", "公共课"),
                new EnrollRecord("S000001", "C000001", "Java程序设计", "专业课"),
                new EnrollRecord("S000001", "C000002", "数据库原理", "专业课"),
                new EnrollRecord("S000003", "C000004", "大学英语", "公共课"),
                new EnrollRecord("S000004", "C000005", "创新创业实践", "选修课")
        );
    }

    private List<EnrollRecord> sortedCopy(List<EnrollRecord> sourceRecords) {
        List<EnrollRecord> result = new ArrayList<>(sourceRecords);
        result.sort(RECORD_COMPARATOR);
        return result;
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private String normalizeCourseType(String courseName, String courseType) {
        String trimmedType = trim(courseType);
        if (VALID_COURSE_TYPES.contains(trimmedType)) {
            return trimmedType;
        }
        return inferCourseType(courseName);
    }

    private String inferCourseType(String courseName) {
        String name = trim(courseName).toLowerCase(Locale.ROOT);
        if (name.contains("java") || name.contains("数据库") || name.contains("计算机")
                || name.contains("程序设计") || name.contains("网络")) {
            return "专业课";
        }
        if (name.contains("英语") || name.contains("思想政治") || name.contains("体育")
                || name.contains("高等数学")) {
            return "公共课";
        }
        return "选修课";
    }

    private String trim(String value) {
        return Objects.toString(value, "").trim();
    }
}
