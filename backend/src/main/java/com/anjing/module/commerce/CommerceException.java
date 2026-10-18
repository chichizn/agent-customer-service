package com.anjing.module.commerce;

import lombok.Getter;

@Getter
public class CommerceException extends RuntimeException {
    private final String errorCode;
    public CommerceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
