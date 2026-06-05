package com.yukai.team.statisticsservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "Generic page response")
public class PagedResponse<T> {

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
