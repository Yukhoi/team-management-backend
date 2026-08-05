package com.yukai.team.matchservice.opponentanalysis.exception;

public class OpponentAnalysisConflictException extends RuntimeException {

    private final String code;

    public OpponentAnalysisConflictException(String message) {
        super(message);
        this.code = "OPPONENT_ANALYSIS_CONFLICT";
    }

    public OpponentAnalysisConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
