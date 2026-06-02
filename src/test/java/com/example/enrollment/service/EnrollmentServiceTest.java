package com.example.enrollment.service;

import com.example.enrollment.entity.EnrollRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentServiceTest {

    @Test
    void importFromCsvDeduplicatesAndSortsByStudentIdThenCourseId() {
        EnrollmentService service = new EnrollmentService();

        ImportResult result = service.importFromCsv("""
                S000002,C000003,计算机网络,公共课
                S000001,C000002,数据库原理,专业课
                S000001,C000001,Java程序设计,专业课
                S000001,C000001,Java开发基础,选修课
                """);

        List<EnrollRecord> records = result.records();
        assertThat(records).hasSize(3);
        assertThat(records.get(0).getStudentId()).isEqualTo("S000001");
        assertThat(records.get(0).getCourseId()).isEqualTo("C000001");
        assertThat(records.get(1).getCourseId()).isEqualTo("C000002");
        assertThat(result.duplicateCount()).isEqualTo(1);
    }

    @Test
    void searchMatchesAllRequiredFields() {
        EnrollmentService service = new EnrollmentService();
        service.importFromCsv("""
                S000001,C000001,Java程序设计,专业课
                S000002,C000003,计算机网络,公共课
                S000003,C000005,创新创业实践,选修课
                """);

        assertThat(service.search("S000001")).hasSize(1);
        assertThat(service.search("C000003")).hasSize(1);
        assertThat(service.search("Java")).hasSize(1);
        assertThat(service.search("选修课")).hasSize(1);
        assertThat(service.search("不存在")).isEmpty();
    }

    @Test
    void importFromCsvRejectsInvalidIdsAndSupportsQuotedComma() {
        EnrollmentService service = new EnrollmentService();

        ImportResult result = service.importFromCsv("""
                S1,C000001,Java程序设计,专业课
                S000001,C001,数据库原理,专业课
                S000002,C000003,"计算机科学导论,A班",专业课
                """);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).getCourseName()).isEqualTo("计算机科学导论,A班");
        assertThat(result.errors()).hasSize(2);
    }

    @Test
    void searchSupportsPaginationForJsonApiUse() {
        EnrollmentService service = new EnrollmentService();

        assertThat(service.search("", 1, 2).records()).hasSize(2);
        assertThat(service.search("", 1, 2).total()).isEqualTo(5);
        assertThat(service.search("", 1, 2).totalPages()).isEqualTo(3);
    }
}
