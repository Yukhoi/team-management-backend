package com.yukai.team.identityservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
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

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
