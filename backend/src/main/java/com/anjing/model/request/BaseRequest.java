package com.anjing.model.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 通用请求基类
 * 
 * <p>所有业务请求类的基类，提供序列化支持和通用字段</p>
 * 
 * <h3>🎯 设计原则：</h3>
 * <ul>
 *   <li>📦 纯净基类 - 只包含真正通用的字段</li>
 *   <li>🔧 序列化支持 - 实现Serializable接口</li>
 *   <li>🎨 统一风格 - 所有业务请求类的统一基础</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Setter
@Getter
public class BaseRequest implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    // 这里只放真正通用的字段
    // 如需要添加通用字段（如操作人ID、请求时间等），在这里添加
}