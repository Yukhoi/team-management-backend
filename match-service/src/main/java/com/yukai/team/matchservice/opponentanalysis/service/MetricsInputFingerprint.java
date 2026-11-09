package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentAnalysisInput;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeMap;

/** Canonical object keys; array order and explicit null availability semantics are preserved. */
public final class MetricsInputFingerprint {
    private final ObjectMapper mapper;

    public MetricsInputFingerprint(ObjectMapper mapper) { this.mapper = mapper; }

    public String fingerprint(OpponentAnalysisInput input) {
        return fingerprint(mapper.valueToTree(input));
    }

    public String fingerprint(JsonNode input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsBytes(canonical(input))));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize canonical AI input", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), canonical(entry.getValue())));
            ObjectNode result = mapper.createObjectNode();
            fields.forEach(result::set);
            return result;
        }
        if (node.isArray()) {
            var result = mapper.createArrayNode();
            node.forEach(value -> result.add(canonical(value)));
            return result;
        }
        if (node.isNumber()) {
            return com.fasterxml.jackson.databind.node.DecimalNode.valueOf(node.decimalValue().stripTrailingZeros());
        }
        return node;
    }
}
