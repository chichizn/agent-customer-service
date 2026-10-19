package com.anjing.module.chat.workbench;

import jakarta.validation.constraints.*;

public final class WorkbenchDTO {
    private WorkbenchDTO() {}
    public record Open(@NotBlank @Size(max=64) String shopId,
                       @NotBlank @Size(max=64) String customerId, boolean forceNew) {}
    public record SessionId(@NotBlank @Size(max=64) String sessionId) {}
    public record Send(@NotBlank @Size(max=64) String sessionId,
                       @NotBlank @Size(max=64) String clientMessageId,
                       @NotBlank @Size(max=4000) String content) {}
    public record Reception(@NotBlank @Size(max=64) String sessionId,
                            @NotBlank @Pattern(regexp="AI|HUMAN") String receptionMode) {}
    public record Reset(@AssertTrue boolean confirmed) {}
}
