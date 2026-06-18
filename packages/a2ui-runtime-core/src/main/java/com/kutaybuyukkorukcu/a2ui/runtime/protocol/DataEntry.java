package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DataEntry(
        @JsonProperty("key") String key,
        @JsonProperty("valueString") @JsonInclude(JsonInclude.Include.NON_NULL) String valueString,
        @JsonProperty("valueNumber") @JsonInclude(JsonInclude.Include.NON_NULL) Number valueNumber,
        @JsonProperty("valueBoolean") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean valueBoolean,
        @JsonProperty("valueMap") @JsonInclude(JsonInclude.Include.NON_NULL) List<DataEntry> valueMap
) {
    public DataEntry {
        boolean isMap = valueMap != null;
        boolean isNumber = valueNumber != null;
        boolean isBoolean = valueBoolean != null;
        long typedFields = (isMap ? 1 : 0) + (isNumber ? 1 : 0) + (isBoolean ? 1 : 0);
        if (typedFields > 1) {
            throw new IllegalArgumentException(
                    "DataEntry must have exactly one value field, but got " + typedFields + " for key: " + key);
        }
        if (typedFields == 1 && valueString != null) {
            throw new IllegalArgumentException(
                    "DataEntry must have exactly one value field, but got multiple for key: " + key);
        }
    }

    public static DataEntry ofString(String key, String value) {
        return new DataEntry(key, value, null, null, null);
    }

    public static DataEntry ofNumber(String key, Number value) {
        return new DataEntry(key, null, value, null, null);
    }

    public static DataEntry ofBoolean(String key, Boolean value) {
        return new DataEntry(key, null, null, value, null);
    }

    public static DataEntry ofMap(String key, List<DataEntry> value) {
        return new DataEntry(key, null, null, null, value);
    }
}