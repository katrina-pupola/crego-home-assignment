package com.crego.backend.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable JSON shape for paginated lists (Spring's Page class is not meant to be serialized directly). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
