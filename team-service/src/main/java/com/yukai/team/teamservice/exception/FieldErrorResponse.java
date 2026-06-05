package com.yukai.team.teamservice.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Field-level error")
public class FieldErrorResponse {

    @Schema(description = "Field name", example = "name")
    private String field;

    @Schema(description = "Field error message", example = "name must not be blank")
    private String message;

    public FieldErrorResponse() {
    }

    public FieldErrorResponse(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
