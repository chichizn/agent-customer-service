package com.anjing.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * SQL日志切面
 * 
 * 监控Repository层的方法调用，配合JPA的SQL日志打印
 * 提供更详细的数据库操作上下文信息
 */
@Aspect
@Component
@Slf4j
public class SqlLogAspect
{

    /**
     * 环绕通知：监控Repository层方法
     */
    @Around("execution(* com.anjing.backend_template.repository..*.*(..))")
    public Object aroundRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求上下文
        String requestInfo = getRequestInfo();
        
        // 获取方法信息
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // 简化类名
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        
        try {
            log.info("🗄️ Repository方法开始 | {} | Method: {}.{} | Args: {}", 
                    requestInfo, simpleClassName, methodName, formatArgs(args));
            
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("✅ Repository方法完成 | {} | Method: {}.{} | 执行时间: {}ms | Result: {}", 
                    requestInfo, simpleClassName, methodName, executionTime, formatResult(result));
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.error("❌ Repository方法异常 | {} | Method: {}.{} | 执行时间: {}ms | Error: {}", 
                    requestInfo, simpleClassName, methodName, executionTime, e.getMessage(), e);
            
            throw e;
        }
    }

    /**
     * 获取请求信息
     */
    private String getRequestInfo() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return String.format("IP: %s | URL: %s %s", 
                        getClientIp(request), 
                        request.getMethod(), 
                        request.getRequestURI());
            }
        } catch (Exception e) {
            // 忽略异常，可能是非Web环境调用
        }
        return "非Web环境调用";
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 格式化参数
     */
    private String formatArgs(Object[] args)
    {
        if (args == null || args.length == 0)
        {
            return "[]";
        }
        
        // 限制参数长度，避免日志过长
        String argsStr = Arrays.toString(args);
        if (argsStr.length() > 200) {
            return argsStr.substring(0, 200) + "...";
        }
        return argsStr;
    }

    /**
     * 格式化返回结果
     */
    private String formatResult(Object result)
    {
        if (result == null) {
            return "null";
        }
        
        String resultStr;
        
        // 特殊处理Collection类型
        if (result instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) result;
            resultStr = String.format("Collection[size=%d]", collection.size());
        } 
        // 特殊处理Page类型
        else if (result.getClass().getSimpleName().contains("Page")) {
            resultStr = String.format("Page[%s]", result.toString());
        } 
        // 其他类型
        else {
            resultStr = result.toString();
        }
        
        // 限制结果长度
        if (resultStr.length() > 200) {
            return resultStr.substring(0, 200) + "...";
        }
        return resultStr;
    }
}
