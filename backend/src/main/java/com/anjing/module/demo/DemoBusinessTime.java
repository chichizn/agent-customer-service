package com.anjing.module.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Only demo business rules use the reference clock; operational timestamps and leases use real time. */
@Service
public class DemoBusinessTime {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final String mode;
    private final Clock systemClock;
    private final Clock referenceClock;

    @Autowired
    public DemoBusinessTime(@Value("${app.demo.time-mode:fixed}") String mode,
                            @Value("${app.demo.reference-time:2026-10-18T12:00:00+08:00}") String referenceTime) {
        this(mode, referenceTime, Clock.system(ZONE));
    }

    DemoBusinessTime(String mode, String referenceTime, Clock systemClock) {
        this.mode = mode.trim().toLowerCase(Locale.ROOT);
        if (!this.mode.equals("fixed") && !this.mode.equals("system")) {
            throw new IllegalArgumentException("app.demo.time-mode must be fixed or system");
        }
        this.systemClock = systemClock;
        this.referenceClock = Clock.fixed(OffsetDateTime.parse(referenceTime).toInstant(), ZONE);
    }

    public LocalDateTime now(boolean demoData) {
        return LocalDateTime.ofInstant((demoData && mode.equals("fixed") ? referenceClock : systemClock).instant(), ZONE);
    }

    public Info describe() {
        return new Info(mode, now(true).atZone(ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), ZONE.getId());
    }

    public record Info(String mode, String effectiveAt, String timeZone) {}
}
