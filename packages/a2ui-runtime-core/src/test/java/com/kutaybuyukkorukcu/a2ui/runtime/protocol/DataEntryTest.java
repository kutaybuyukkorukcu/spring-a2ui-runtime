package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataEntryTest {

    @Test
    void shouldCreateStringEntry() {
        DataEntry entry = DataEntry.ofString("name", "Alice");
        assertThat(entry.key()).isEqualTo("name");
        assertThat(entry.valueString()).isEqualTo("Alice");
        assertThat(entry.valueNumber()).isNull();
        assertThat(entry.valueBoolean()).isNull();
        assertThat(entry.valueMap()).isNull();
    }

    @Test
    void shouldCreateNumberEntry() {
        DataEntry entry = DataEntry.ofNumber("count", 42);
        assertThat(entry.key()).isEqualTo("count");
        assertThat(entry.valueNumber()).isEqualTo(42);
        assertThat(entry.valueString()).isNull();
    }

    @Test
    void shouldCreateBooleanEntry() {
        DataEntry entry = DataEntry.ofBoolean("active", true);
        assertThat(entry.key()).isEqualTo("active");
        assertThat(entry.valueBoolean()).isTrue();
    }

    @Test
    void shouldCreateMapEntry() {
        DataEntry inner = DataEntry.ofString("street", "123 Main");
        DataEntry entry = DataEntry.ofMap("address", List.of(inner));
        assertThat(entry.key()).isEqualTo("address");
        assertThat(entry.valueMap()).hasSize(1);
        assertThat(entry.valueMap().get(0).key()).isEqualTo("street");
    }

    @Test
    void shouldRejectZeroValueFields() {
        assertThatThrownBy(() -> new DataEntry("key", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one value field");
    }

    @Test
    void shouldRejectMultipleValueFields() {
        assertThatThrownBy(() -> new DataEntry("key", "str", 42, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one value field");
    }
}