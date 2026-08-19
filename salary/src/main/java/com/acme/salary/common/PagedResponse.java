package com.acme.salary.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(List<T> items, long totalElements, int page, int size) {

    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
