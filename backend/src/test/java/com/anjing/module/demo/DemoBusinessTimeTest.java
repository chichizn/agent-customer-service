package com.anjing.module.demo;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.*;

class DemoBusinessTimeTest {
    private static final Clock SYSTEM = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final String REFERENCE = "2026-10-18T12:00:00+08:00";

    @Test void fixedModeKeepsDemoRulesStableWhileOtherDataUsesSystemTime() {
        var time = new DemoBusinessTime("fixed", REFERENCE, SYSTEM);
        assertThat(time.now(true)).isEqualTo(LocalDateTime.parse("2026-10-18T12:00:00"));
        assertThat(time.now(false)).isEqualTo(LocalDateTime.parse("2030-01-01T08:00:00"));
        assertThat(time.describe()).isEqualTo(new DemoBusinessTime.Info("fixed", REFERENCE, "Asia/Shanghai"));
    }

    @Test void systemModeUsesRealTimeForBothKindsOfData() {
        var time = new DemoBusinessTime("system", REFERENCE, SYSTEM);
        assertThat(time.now(true)).isEqualTo(time.now(false));
        assertThat(time.describe().effectiveAt()).isEqualTo("2030-01-01T08:00:00+08:00");
    }

    @Test void referenceTimeIsNormalizedFromItsExplicitOffset() {
        var time = new DemoBusinessTime("fixed", "2026-10-18T04:00:00Z", SYSTEM);
        assertThat(time.describe().effectiveAt()).isEqualTo(REFERENCE);
    }

    @Test void invalidModeOrReferenceTimeFailsAtStartup() {
        assertThatThrownBy(() -> new DemoBusinessTime("typo", REFERENCE, SYSTEM)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DemoBusinessTime("fixed", "2026-10-18T12:00:00", SYSTEM))
            .isInstanceOf(java.time.format.DateTimeParseException.class);
    }
}
