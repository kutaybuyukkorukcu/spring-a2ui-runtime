package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BoundValue(
        @JsonProperty("literalString") @JsonInclude(JsonInclude.Include.NON_NULL) String literalString,
        @JsonProperty("literalNumber") @JsonInclude(JsonInclude.Include.NON_NULL) Number literalNumber,
        @JsonProperty("literalBoolean") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean literalBoolean,
        @JsonProperty("literalArray") @JsonInclude(JsonInclude.Include.NON_NULL) java.util.List<String> literalArray,
        @JsonProperty("path") @JsonInclude(JsonInclude.Include.NON_NULL) String path
) {
    public BoundValue {
        long nonNullValues = 0;
        if (literalString != null) nonNullValues++;
        if (literalNumber != null) nonNullValues++;
        if (literalBoolean != null) nonNullValues++;
        if (literalArray != null) nonNullValues++;
        if (path != null) nonNullValues++;
        if (nonNullValues == 0) {
            throw new IllegalArgumentException("BoundValue must have at least one property set");
        }
    }

    public static BoundValue literalString(String value) {
        return new BoundValue(value, null, null, null, null);
    }

    public static BoundValue literalNumber(Number value) {
        return new BoundValue(null, value, null, null, null);
    }

    public static BoundValue literalBoolean(Boolean value) {
        return new BoundValue(null, null, value, null, null);
    }

    public static BoundValue literalArray(java.util.List<String> value) {
        return new BoundValue(null, null, null, value, null);
    }

    public static BoundValue dynamic(String path) {
        return new BoundValue(null, null, null, null, path);
    }

    public static BoundValue initWithString(String path, String literalValue) {
        return new BoundValue(literalValue, null, null, null, path);
    }

    public static BoundValue initWithNumber(String path, Number literalValue) {
        return new BoundValue(null, literalValue, null, null, path);
    }

    public static BoundValue initWithBoolean(String path, Boolean literalValue) {
        return new BoundValue(null, null, literalValue, null, path);
    }
}