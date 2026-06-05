package com.yukai.team.matchservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Generic page response")
public class PageResponse<T> {

    @Schema(description = "Page content")
    private List<T> content;
    @Schema(description = "Current zero-based page index", example = "0")
    private int page;
    @Schema(description = "Page size", example = "20")
    private int size;
    @Schema(description = "Total element count", example = "42")
    private long totalElements;
    @Schema(description = "Total page count", example = "3")
    private int totalPages;
}
