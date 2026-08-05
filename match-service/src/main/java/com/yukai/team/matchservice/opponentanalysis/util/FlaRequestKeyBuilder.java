package com.yukai.team.matchservice.opponentanalysis.util;

public final class FlaRequestKeyBuilder {

    private FlaRequestKeyBuilder() {
    }

    public static String buildClassementRequestKey(Long championnatId, Long saisonId) {
        return String.valueOf(championnatId) + saisonId;
    }
}
