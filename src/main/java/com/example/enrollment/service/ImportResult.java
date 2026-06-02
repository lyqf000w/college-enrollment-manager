package com.example.enrollment.service;

import com.example.enrollment.entity.EnrollRecord;

import java.util.List;

public record ImportResult(
        List<EnrollRecord> records,
        int inputCount,
        int validCount,
        int duplicateCount,
        List<String> errors
) {
}
