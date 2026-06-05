package com.yukai.team.tournamentservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
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
    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;

    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
