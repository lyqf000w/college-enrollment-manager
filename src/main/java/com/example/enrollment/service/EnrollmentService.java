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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final List<String> VALID_COURSE_TYPES = List.of("公共课", "专业课", "选修课");
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("S\\d{6}");
    private static final Pattern COURSE_ID_PATTERN = Pattern.compile("C\\d{6}");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Comparator<EnrollRecord> RECORD_COMPARATOR = Comparator
            .comparing(EnrollRecord::getStudentId)
            .thenComparing(EnrollRecord::getCourseId);

    private final AtomicReference<List<EnrollRecord>> records = new AtomicReference<>(List.of());

    public EnrollmentService() {
        resetToSampleRecords();
    }

    public List<EnrollRecord> resetToSampleRecords() {
        records.set(List.copyOf(processRecords(sampleRecords())));
        return getAllRecords();
    }

    public List<EnrollRecord> clearRecords() {
        records.set(List.of());
        return List.of();
    }

    public List<EnrollRecord> getAllRecords() {
        return sortedCopy(records.get());
    }

    public PagedResult<EnrollRecord> getAllRecords(int page, int size) {
        return toPage(getAllRecords(), page, size);
    }

    /**
     * 页面导入采用覆盖策略：每次导入成功后，用本次有效数据替换内存中的全量记录。
     */
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

            CsvLine fields = parseCsvLine(line);
            if (fields.malformed()) {
                errors.add("第 " + (i + 1) + " 行CSV引号未闭合");
                continue;
            }
            if (fields.values().size() < 3 || fields.values().size() > 4) {
                errors.add("第 " + (i + 1) + " 行格式错误，应为：学生ID,课程ID,课程名称,课程类型");
                continue;
            }

            String studentId = fields.values().get(0).trim();
            String courseId = fields.values().get(1).trim();
            String courseName = fields.values().get(2).trim();
            String courseType = fields.values().size() == 4 ? fields.values().get(3).trim() : "";

            if (studentId.isEmpty() || courseId.isEmpty() || courseName.isEmpty()) {
                errors.add("第 " + (i + 1) + " 行存在必填字段为空");
                continue;
            }
            if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
                errors.add("第 " + (i + 1) + " 行学生ID格式错误，应为 S+6位数字");
                continue;
            }
            if (!COURSE_ID_PATTERN.matcher(courseId).matches()) {
                errors.add("第 " + (i + 1) + " 行课程ID格式错误，应为 C+6位数字");
                continue;
            }

            parsedRecords.add(new EnrollRecord(studentId, courseId, courseName, normalizeCourseType(courseName, courseType)));
        }

        if (inputCount == 0) {
            errors.add("CSV内容为空，请至少输入一条选课记录");
        }
        if (parsedRecords.isEmpty()) {
            return new ImportResult(getAllRecords(), inputCount, 0, 0, errors);
        }

        List<EnrollRecord> processedRecords = processRecords(parsedRecords);
        records.set(List.copyOf(processedRecords));

        int duplicateCount = Math.max(0, parsedRecords.size() - processedRecords.size());
        return new ImportResult(getAllRecords(), inputCount, parsedRecords.size(), duplicateCount, errors);
    }

    public List<EnrollRecord> search(String keyword) {
        String normalizedKeyword = Objects.toString(keyword, "").trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return getAllRecords();
        }

        return records.get().stream()
                .filter(record -> containsIgnoreCase(record.getStudentId(), normalizedKeyword)
                        || containsIgnoreCase(record.getCourseId(), normalizedKeyword)
                        || containsIgnoreCase(record.getCourseName(), normalizedKeyword)
                        || containsIgnoreCase(record.getCourseType(), normalizedKeyword))
                .sorted(RECORD_COMPARATOR)
                .collect(Collectors.toList());
    }

    public PagedResult<EnrollRecord> search(String keyword, int page, int size) {
        return toPage(search(keyword), page, size);
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

    private PagedResult<EnrollRecord> toPage(List<EnrollRecord> sourceRecords, int page, int size) {
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.min(Math.max(1, size <= 0 ? DEFAULT_PAGE_SIZE : size), MAX_PAGE_SIZE);
        int total = sourceRecords.size();
        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, total);
        int toIndex = Math.min(fromIndex + normalizedSize, total);
        return new PagedResult<>(
                sourceRecords.subList(fromIndex, toIndex),
                normalizedPage,
                normalizedSize,
                total,
                (int) Math.ceil(total / (double) normalizedSize)
        );
    }

    private CsvLine parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        fields.add(current.toString());
        return new CsvLine(fields, quoted);
    }

    private record CsvLine(List<String> values, boolean malformed) {
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
        if (containsAny(name, "java", "python", "c++", "程序设计", "数据结构", "算法", "数据库",
                "mysql", "nosql", "计算机", "网络", "tcp/ip", "软件工程", "操作系统")) {
            return "专业课";
        }
        if (containsAny(name, "英语", "思想政治", "体育", "高等数学", "大学语文", "马克思", "毛概")) {
            return "公共课";
        }
        return "选修课";
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String trim(String value) {
        return Objects.toString(value, "").trim();
    }
}
