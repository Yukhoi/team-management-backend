package com.yukai.team.matchservice.opponentanalysis.exception;

import org.springframework.http.HttpStatus;

public class FlaClientException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public FlaClientException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public FlaClientException(HttpStatus httpStatus, String code, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
