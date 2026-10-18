package com.anjing.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SQL日志工具类
 * 
 * 提供SQL执行日志的美化打印功能
 * 类似于之前MyBatis拦截器的效果
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Slf4j
public class SqlLogUtils {

    private SqlLogUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 打印SQL执行日志
     * 
     * @param methodName   方法名
     * @param className    类名
     * @param args         参数
     * @param result       结果
     * @param executionTime 执行时间
     */
    public static void logSqlExecution(String methodName, String className, Object[] args, Object result, long executionTime) {
        try {
            RequestInfo requestInfo = getRequestInfo();
            
            // 美化打印样式，借鉴Agent X的格式
            log.info("\n----------------------------------------------------------\n\t{}{}{}{}{}{}{}{}",
                    " << JPA Repository SQL Log >>",
                    "\n\t [Request IP]    \t:    \t" + requestInfo.getIp(),
                    "\n\t [Request URL]   \t:    \t" + requestInfo.getUrl(),
                    "\n\t [Request Method]\t:    \t" + requestInfo.getMethod(),
                    "\n\t [Repository]    \t:    \t" + getSimpleClassName(className),
                    "\n\t [Method]        \t:    \t" + methodName,
                    "\n\t [Parameters]    \t:    \t" + formatParameters(args),
                    "\n\t [Execution Time]\t:    \t" + executionTime + "ms",
                    "\n\t [Result]        \t:    \t" + formatResult(result),
                    "\n----------------------------------------------------------\n");
            
        } catch (Exception e) {
            log.warn("打印SQL日志失败", e);
        }
    }

    /**
     * 打印简化的SQL执行日志
     */
    public static void logSimpleSqlExecution(String operation, long executionTime) {
        try {
            RequestInfo requestInfo = getRequestInfo();
            
            log.info("🗄️ SQL执行 | {} | Operation: {} | 执行时间: {}ms", 
                    requestInfo.getSimpleInfo(), operation, executionTime);
                    
        } catch (Exception e) {
            log.warn("打印简化SQL日志失败", e);
        }
    }

    /**
     * 获取请求信息
     */
    private static RequestInfo getRequestInfo() {
        RequestInfo info = new RequestInfo();
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                info.setIp(getClientIp(request));
                info.setUrl(request.getRequestURL().toString());
                info.setMethod(request.getMethod() + " " + request.getRequestURI());
            } else {
                info.setIp("Non-Web");
                info.setUrl("Non-Web Environment");
                info.setMethod("Non-Web");
            }
        } catch (Exception e) {
            info.setIp("Unknown");
            info.setUrl("Unknown");
            info.setMethod("Unknown");
        }
        
        return info;
    }

    /**
     * 获取客户端真实IP
     */
    private static String getClientIp(HttpServletRequest request) {
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
     * 获取简单类名
     */
    private static String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "Unknown";
        }
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    /**
     * 格式化参数
     */
    private static String formatParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                // 字符串参数用引号包围
                sb.append("'").append(arg).append("'");
            } else if (arg instanceof Number || arg instanceof Boolean) {
                // 数字和布尔值直接显示
                sb.append(arg);
            } else {
                // 其他对象显示类型和toString
                sb.append(arg.getClass().getSimpleName()).append("(").append(arg).append(")");
            }
        }
        
        sb.append("]");
        
        // 限制长度
        String result = sb.toString();
        if (result.length() > 300) {
            return result.substring(0, 300) + "...]";
        }
        
        return result;
    }

    /**
     * 格式化结果
     */
    private static String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        if (result instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) result;
            return String.format("Collection[size=%d, type=%s]", 
                    collection.size(), result.getClass().getSimpleName());
        }
        
        if (result instanceof java.util.Optional) {
            java.util.Optional<?> optional = (java.util.Optional<?>) result;
            return String.format("Optional[present=%s]", optional.isPresent());
        }
        
        if (result.getClass().getSimpleName().contains("Page")) {
            return String.format("Page[%s]", result.toString());
        }
        
        if (result instanceof Number || result instanceof Boolean || result instanceof String) {
            return result.toString();
        }
        
        return String.format("%s(%s)", result.getClass().getSimpleName(), result.toString());
    }

    /**
     * 请求信息类
     */
    private static class RequestInfo {
        private String ip;
        private String url;
        private String method;

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getSimpleInfo() {
            return String.format("IP: %s | %s", ip, method);
        }
    }
}
