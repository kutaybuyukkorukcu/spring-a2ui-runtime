package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundValueTest {

    @Test
    void shouldCreateLiteralString() {
        BoundValue bv = BoundValue.literalString("hello");
        assertThat(bv.literalString()).isEqualTo("hello");
        assertThat(bv.literalNumber()).isNull();
        assertThat(bv.literalBoolean()).isNull();
        assertThat(bv.literalArray()).isNull();
        assertThat(bv.path()).isNull();
    }

    @Test
    void shouldCreateLiteralNumber() {
        BoundValue bv = BoundValue.literalNumber(42);
        assertThat(bv.literalNumber()).isEqualTo(42);
        assertThat(bv.literalString()).isNull();
    }

    @Test
    void shouldCreateLiteralBoolean() {
        BoundValue bv = BoundValue.literalBoolean(true);
        assertThat(bv.literalBoolean()).isTrue();
        assertThat(bv.literalString()).isNull();
    }

    @Test
    void shouldCreateLiteralArray() {
        BoundValue bv = BoundValue.literalArray(java.util.List.of("a", "b"));
        assertThat(bv.literalArray()).containsExactly("a", "b");
    }

    @Test
    void shouldCreateDynamic() {
        BoundValue bv = BoundValue.dynamic("/user/name");
        assertThat(bv.path()).isEqualTo("/user/name");
        assertThat(bv.literalString()).isNull();
    }

    @Test
    void shouldCreateInitWithString() {
        BoundValue bv = BoundValue.initWithString("/user/name", "Guest");
        assertThat(bv.path()).isEqualTo("/user/name");
        assertThat(bv.literalString()).isEqualTo("Guest");
    }

    @Test
    void shouldCreateInitWithNumber() {
        BoundValue bv = BoundValue.initWithNumber("/counter", 0);
        assertThat(bv.path()).isEqualTo("/counter");
        assertThat(bv.literalNumber()).isEqualTo(0);
    }

    @Test
    void shouldCreateInitWithBoolean() {
        BoundValue bv = BoundValue.initWithBoolean("/active", false);
        assertThat(bv.path()).isEqualTo("/active");
        assertThat(bv.literalBoolean()).isFalse();
    }

    @Test
    void shouldRejectEmptyBoundValue() {
        assertThatThrownBy(() -> new BoundValue(null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAllowPathOnly() {
        BoundValue bv = BoundValue.dynamic("/data/value");
        assertThat(bv.path()).isEqualTo("/data/value");
        assertThat(bv.literalString()).isNull();
    }

    @Test
    void shouldAllowPathPlusLiteral() {
        BoundValue bv = BoundValue.initWithString("/data/name", "default");
        assertThat(bv.path()).isEqualTo("/data/name");
        assertThat(bv.literalString()).isEqualTo("default");
    }
}