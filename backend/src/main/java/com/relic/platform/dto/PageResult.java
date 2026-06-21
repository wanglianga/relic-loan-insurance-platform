package com.relic.platform.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;

    public static <T> PageResult<T> from(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setContent(page.getContent());
        result.setTotalElements(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setNumber(page.getNumber());
        result.setSize(page.getSize());
        return result;
    }
}
