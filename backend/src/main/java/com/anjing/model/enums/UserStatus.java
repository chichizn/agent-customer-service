package com.anjing.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    /**
     * 激活状态
     */
    ACTIVE("ACTIVE", "激活"),

    /**
     * 禁用状态
     */
    DISABLED("DISABLED", "禁用"),

    /**
     * 锁定状态
     */
    LOCKED("LOCKED", "锁定"),

    /**
     * 待激活状态
     */
    PENDING("PENDING", "待激活");

    /**
     * 状态码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     * 
     * @param code 状态码
     * @return 用户状态枚举
     */
    public static UserStatus fromCode(String code) {
        for (UserStatus status : UserStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态码: " + code);
    }

    /**
     * 检查是否为有效状态
     * 
     * @return 是否为有效状态
     */
    public boolean isValid() {
        return this == ACTIVE;
    }
}
