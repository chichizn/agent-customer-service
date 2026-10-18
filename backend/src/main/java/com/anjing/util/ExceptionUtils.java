package com.anjing.util;

import com.anjing.model.exception.BizException;
import com.anjing.model.exception.SystemException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 异常工具类
 * 1、默认参数进行倒打堆栈
 * 2、倒打堆栈
 */
@Component
@Slf4j
public class ExceptionUtils
{
    // ForwardCounter 类定义
    public static class ForwardCounter
    {
        public int i = 0;
    }

    /**
     * 打印详细异常信息
     * 
     * @param t 异常对象
     * @param request HTTP请求对象（可为null）
     */
    public static void printException(Throwable t, HttpServletRequest request)
    {
        if (t == null)
        {
            log.warn("异常对象为null，跳过异常信息打印");
            return;
        }
        
        try
        {
            StringBuilder sb = new StringBuilder();
            
            // === 异常概要信息 ===
            buildExceptionSummary(sb, t, request);
            
            // === 异常堆栈信息 ===
            sb.append("\n📋 STACK TRACE DETAILS:\n");
            ForwardCounter counter = new ForwardCounter();
            recursiveReversePrintStackCause(t, 5, counter, 5, sb);
            
            // === 统一输出 ===
            log.error("\n==================== ENHANCED EXCEPTION INFO BEGIN ====================");
            log.error(sb.toString());
            log.error("==================== ENHANCED EXCEPTION INFO END ====================\n");
            
        } catch (Exception e) {
            // 确保异常处理本身不会抛出异常
            log.error("打印异常信息时发生错误，回退到基础异常处理: {}", e.getMessage());
            recursiveReversePrintStackCauseCommon(t);
        }
    }
    
    /**
     * 构建异常概要信息
     */
    private static void buildExceptionSummary(StringBuilder sb, Throwable t, HttpServletRequest request) {
        sb.append("\n🚨 EXCEPTION SUMMARY 🚨\n");
        sb.append("┌─────────────────────────────────────────────────────────────────\n");
        
        // 时间
        sb.append("│ Time: ").append(formatCurrentTime()).append("\n");
        
        // 异常类型
        sb.append("│ Exception: ").append(safeGet(t.getClass().getSimpleName(), "Unknown")).append("\n");
        
        // 异常消息
        sb.append("│ Message: ").append(safeGet(t.getMessage(), "No message")).append("\n");
        
        // ErrorCode信息（如果是自定义异常）
        buildErrorCodeInfo(sb, t);
        
        // 请求信息
        buildRequestInfo(sb, request);
        
        sb.append("└─────────────────────────────────────────────────────────────────\n");
    }
    
    /**
     * 构建ErrorCode信息
     */
    private static void buildErrorCodeInfo(StringBuilder sb, Throwable t) {
        try {
            if (t instanceof BizException) {
                BizException bizEx = (BizException) t;
                if (bizEx.getErrorCode() != null) {
                    sb.append("│ Error Code: ").append(safeGet(bizEx.getErrorCode().getCode(), "N/A")).append("\n");
                    sb.append("│ Error Message: ").append(safeGet(bizEx.getErrorCode().getMessage(), "N/A")).append("\n");
                    sb.append("│ Category: Business Exception\n");
                }
            } else if (t instanceof SystemException) {
                SystemException sysEx = (SystemException) t;
                if (sysEx.getErrorCode() != null) {
                    sb.append("│ Error Code: ").append(safeGet(sysEx.getErrorCode().getCode(), "N/A")).append("\n");
                    sb.append("│ Error Message: ").append(safeGet(sysEx.getErrorCode().getMessage(), "N/A")).append("\n");
                    sb.append("│ Category: System Exception\n");
                }
            } else {
                sb.append("│ Category: Standard Exception\n");
            }
        } catch (Exception e) {
            sb.append("│ Category: Error retrieving category\n");
        }
    }
    
    /**
     * 构建请求信息
     */
    private static void buildRequestInfo(StringBuilder sb, HttpServletRequest request) {
        if (request == null) {
            sb.append("│ Request: No request context\n");
            return;
        }
        
        try {
            // 请求基本信息
            String method = safeGet(request.getMethod(), "Unknown");
            String uri = safeGet(request.getRequestURI(), "Unknown");
            String clientIp = getClientIpAddress(request);
            String client = identifyClient(request.getHeader("User-Agent"));
            
            sb.append("│ Request: ").append(method).append(" ").append(uri).append("\n");
            sb.append("│ Client IP: ").append(clientIp).append("\n");
            sb.append("│ Client: ").append(client).append("\n");
            
            // 用户信息（如果有）
            String userInfo = getUserInfo(request);
            if (!"Unknown".equals(userInfo))
            {
                sb.append("│ User: ").append(userInfo).append("\n");
            }
            
        } catch (Exception e) {
            sb.append("│ Request: Error retrieving request info\n");
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "Unknown";
        
        try {
            // X-Forwarded-For
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (isValidIp(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            // X-Real-IP
            String xRealIp = request.getHeader("X-Real-IP");
            if (isValidIp(xRealIp)) {
                return xRealIp;
            }
            
            // Remote Address
            String remoteAddr = request.getRemoteAddr();
            return safeGet(remoteAddr, "Unknown");
            
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * 识别客户端类型
     */
    private static String identifyClient(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "Unknown";
        }
        
        try {
            String ua = userAgent.toLowerCase();
            if (ua.contains("postmanruntime")) return "Postman";
            if (ua.contains("curl")) return "cURL";
            if (ua.contains("micromessenger")) return "WeChat";
            if (ua.contains("mobile")) return "Mobile Browser";
            if (ua.contains("chrome")) return "Chrome";
            if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
            if (ua.contains("firefox")) return "Firefox";
            if (ua.contains("edge")) return "Edge";
            return "Other Client";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * 获取用户信息
     */
    private static String getUserInfo(HttpServletRequest request) {
        if (request == null) return "Unknown";
        
        try {
            String userId = request.getHeader("X-User-Id");
            String username = request.getHeader("X-Username");
            
            if (userId != null && !userId.trim().isEmpty()) {
                if (username != null && !username.trim().isEmpty()) {
                    return userId + "(" + username + ")";
                } else {
                    return userId;
                }
            }
            return "Anonymous";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * 格式化当前时间
     */
    private static String formatCurrentTime() {
        try {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        } catch (Exception e) {
            return "Unknown Time";
        }
    }
    
    /**
     * 安全获取字符串，避免null
     */
    private static String safeGet(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    
    /**
     * 验证IP地址是否有效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
    public static void recursiveReversePrintStackCauseCommon(Throwable t)
    {
        StringBuilder sb        = new StringBuilder();  //
        ForwardCounter counter  = new ForwardCounter(); //
        int causeDepth = 5;     // 递归打印的cause的最大深度
        int stackDepth = 5;     // 每一个异常栈的打印深度
        recursiveReversePrintStackCause(t,causeDepth,counter,stackDepth,sb);
        log.error("\n---------------------reverse exception stackTrace begin---------------------\n");
        log.error("\n" +sb.toString());
        log.error("\n---------------------reverse exception stackTrace end---------------------\n");
    }

    /**
     * 2、倒打堆栈：递归逆向打印堆栈及cause(即从最底层的异常开始往上打)
     * @param t 原始异常
     * @param causeDepth 需要递归打印的cause的最大深度
     * @param counter 当前打印的cause的深度计数器(这里必须用引用类型，如果用基本数据类型，你对计数器的修改只能对当前栈帧可见，但是这个计数器，又必须在所有栈帧中可见，所以只能用引用类型)
     * @param stackDepth 每一个异常栈的打印深度
     * @param sb 字符串构造器
     */
    public static void recursiveReversePrintStackCause(Throwable t, int causeDepth, ForwardCounter counter, int stackDepth, StringBuilder sb)
    {
        if(t == null)
        {
            return;
        }
        if (t.getCause() != null)
        {
            recursiveReversePrintStackCause(t.getCause(), causeDepth, counter, stackDepth, sb);
        }
        if(counter.i++ < causeDepth){
            doPrintStack(t, stackDepth, sb);
        }
    }

    // 处理堆栈信息
    public static void doPrintStack(Throwable t, int stackDepth, StringBuilder sb)
    {
        StackTraceElement[] stackTraceElements = t.getStackTrace();
        if(sb.lastIndexOf("\t") > -1)
        {
            sb.deleteCharAt(sb.length()-1);
            sb.append("Caused: ");
        }
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n\t");
        for(int i=0; i < stackDepth; ++i)
        {
            if(i >= stackTraceElements.length){
                break;
            }
            StackTraceElement element = stackTraceElements[i];
             sb.append(reduceClassName(element.getClassName()))
             // sb.append(element.getClassName())
                    .append("[")
                    .append(element.getMethodName())
                    .append(":")
                    .append(element.getLineNumber())
                    .append("]")
                    .append("\n\t");
        }
    }

    // 简化类名以便更好地阅读
    private static String reduceClassName(String className)
    {
        String[] parts = className.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++)
        {
            sb.append(parts[i].charAt(0)).append(".");
        }
        sb.append(parts[parts.length - 1]);
        return sb.toString();
    }

}
