package com.anjing.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 统一日志处理切面
 * 
 * 功能：
 * 1. 记录Controller接口的入参、出参
 * 2. 记录接口执行时间
 * 3. 记录请求信息（IP、URL、方法等）
 * 4. 异常日志处理
 * 
 * 通过AOP面向切面编程，零侵入完成对接口信息的监控
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerLogAspect
{

    private final ObjectMapper objectMapper;

    /**
     * 线程本地变量存储请求上下文
     */
    private final ThreadLocal<RequestContext> requestContextHolder = new ThreadLocal<>();

    /**
     * 定义切点：Controller包下的所有方法
     */
    @Pointcut("execution(public * com.anjing.controller..*.*(..))")
    public void controllerMethod() {}

    /**
     * 前置通知：记录请求信息和参数
     */
    @Before("controllerMethod()")
    public void beforeMethod(JoinPoint joinPoint)
    {
        try
        {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {return;}

            HttpServletRequest request = attributes.getRequest();
            
            // 创建请求上下文
            RequestContext context = new RequestContext();
            context.setStartTime(System.currentTimeMillis());
            context.setRequestId(generateRequestId());
            context.setUrl(request.getRequestURL().toString());
            context.setMethod(request.getMethod());
            context.setIp(getClientIp(request));
            context.setUserAgent(request.getHeader("User-Agent"));
            context.setClassName(joinPoint.getSignature().getDeclaringTypeName());
            context.setMethodName(joinPoint.getSignature().getName());
            context.setArgs(joinPoint.getArgs());
            
            requestContextHolder.set(context);

            // 记录请求日志
            log.info("🚀 API请求开始 | RequestId: {} | IP: {} | {} {} | Method: {}.{} | Args: {}",
                    context.getRequestId(),
                    context.getIp(),
                    context.getMethod(),
                    context.getUrl(),
                    getSimpleClassName(context.getClassName()),
                    context.getMethodName(),
                    formatArgs(context.getArgs()));

        } catch (Exception e)
        {
            log.error("记录请求日志失败", e);
        }
    }

    /**
     * 环绕通知：记录执行时间和返回结果
     */
    @Around("controllerMethod()")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable
    {
        RequestContext context = requestContextHolder.get();
        if (context == null)
        {
            return joinPoint.proceed();
        }

        try {
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - context.getStartTime();
            
            // 记录成功日志
            log.info("✅ API请求成功 | RequestId: {} | Method: {}.{} | 执行时间: {}ms | Result: {}",
                    context.getRequestId(),
                    getSimpleClassName(context.getClassName()),
                    context.getMethodName(),
                    executionTime,
                    formatResult(result));
            
            return result;
            
        } catch (Exception e) {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - context.getStartTime();
            
            // 记录异常日志
            log.error("❌ API请求异常 | RequestId: {} | Method: {}.{} | 执行时间: {}ms | Error: {}",
                    context.getRequestId(),
                    getSimpleClassName(context.getClassName()),
                    context.getMethodName(),
                    executionTime,
                    e.getMessage(), e);
            
            throw e;
        } finally {
            // 清理线程本地变量
            requestContextHolder.remove();
        }
    }

    /**
     * 后置通知：清理资源
     */
    @After("controllerMethod()")
    public void afterMethod() {
        requestContextHolder.remove();
    }

    /**
     * 异常通知：记录异常信息
     */
    @AfterThrowing(pointcut = "controllerMethod()", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, Exception exception) {
        RequestContext context = requestContextHolder.get();
        if (context != null) {
            long executionTime = System.currentTimeMillis() - context.getStartTime();
            
            log.error("💥 API异常通知 | RequestId: {} | Method: {}.{} | 执行时间: {}ms | Exception: {}",
                    context.getRequestId(),
                    getSimpleClassName(context.getClassName()),
                    context.getMethodName(),
                    executionTime,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
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
        
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 获取简单类名
     */
    private String getSimpleClassName(String fullClassName) {
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    /**
     * 格式化参数
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            // 过滤敏感信息
            Object[] filteredArgs = Arrays.stream(args)
                    .map(this::filterSensitiveData)
                    .toArray();
            
            String jsonArgs = objectMapper.writeValueAsString(filteredArgs);
            
            // 限制日志长度
            if (jsonArgs.length() > 1000) {
                return jsonArgs.substring(0, 1000) + "...";
            }
            return jsonArgs;
            
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    /**
     * 格式化返回结果
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        try {
            String jsonResult = objectMapper.writeValueAsString(result);
            
            // 限制日志长度
            if (jsonResult.length() > 1000) {
                return jsonResult.substring(0, 1000) + "...";
            }
            return jsonResult;
            
        } catch (Exception e) {
            return result.toString();
        }
    }

    /**
     * 过滤敏感数据
     */
    private Object filterSensitiveData(Object arg) {
        if (arg == null) {
            return null;
        }
        
        String argStr = arg.toString();
        
        // 过滤密码字段
        if (argStr.contains("password") || argStr.contains("Password")) {
            return "***FILTERED***";
        }
        
        // 过滤token字段
        if (argStr.contains("token") || argStr.contains("Token")) {
            return "***FILTERED***";
        }
        
        return arg;
    }

    /**
     * 请求上下文
     */
    private static class RequestContext {
        private long startTime;
        private String requestId;
        private String url;
        private String method;
        private String ip;
        private String userAgent;
        private String className;
        private String methodName;
        private Object[] args;

        // getters and setters
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public Object[] getArgs() { return args; }
        public void setArgs(Object[] args) { this.args = args; }
    }
}
