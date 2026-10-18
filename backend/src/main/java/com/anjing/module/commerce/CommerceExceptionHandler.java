package com.anjing.module.commerce;

import com.anjing.model.result.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CommerceController.class)
public class CommerceExceptionHandler {
    public record ErrorDetail(String errorCode) {}
    @ExceptionHandler(CommerceException.class)
    public ResponseEntity<R<ErrorDetail>> business(CommerceException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "NOT_FOUND", "DEMO_NOT_LOADED" -> HttpStatus.NOT_FOUND;
            case "IDEMPOTENCY_CONFLICT", "STALE_STATUS", "INVALID_TRANSITION", "DEMO_DATA_CONFLICT" -> HttpStatus.CONFLICT;
            case "AGENT_GUARD_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return failure(status, ex.getErrorCode(), ex.getMessage());
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<R<ErrorDetail>> invalid(Exception ex) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "参数缺失或格式不正确；提交工单前需明确确认");
    }
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<R<ErrorDetail>> persistence(Exception ex) {
        log.error("Commerce 持久化操作失败", ex);
        return failure(HttpStatus.SERVICE_UNAVAILABLE, "PERSISTENCE_FAILED", "业务操作未成功，请稍后使用原消息编号重试或查询确认");
    }
    private ResponseEntity<R<ErrorDetail>> failure(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new R<>(status.value(), message, new ErrorDetail(code)));
    }
}
