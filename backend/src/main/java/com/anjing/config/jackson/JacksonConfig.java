package com.anjing.config.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson配置类
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Configuration
public class JacksonConfig
{

    /**
     * 自定义ObjectMapper
     * 
     * @return ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册Java时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 允许空字符串转为null
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        // 设置属性命名策略（如果需要下划线命名）
        // mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        return mapper;
    }

}
