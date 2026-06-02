package com.example.enrollment.service;

import java.util.List;

public record PagedResult<T>(
        List<T> records,
        int page,
        int size,
        int total,
        int totalPages
) {
}
