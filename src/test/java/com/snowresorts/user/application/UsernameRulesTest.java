package com.snowresorts.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsernameRulesTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("normalize strips @ prefix and lowercases")
    void normalize_stripsAtPrefix() {
        assertThat(UsernameRules.normalize("@Rider_01")).isEqualTo("rider_01");
    }

    @Test
    @DisplayName("defaultBase derives from email local-part")
    void defaultBase_fromEmail() {
        assertThat(UsernameRules.defaultBase("New.Rider@snow-resorts.com", USER_ID)).isEqualTo("newrider");
    }
}
