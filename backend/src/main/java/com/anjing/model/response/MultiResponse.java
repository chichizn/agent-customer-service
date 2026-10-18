package com.anjing.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 多数据响应基类
 * 
 * <p>用于返回多个数据项的响应场景</p>
 * 
 * @param <T> 数据类型
 * @author Backend Template Team  
 * @version 1.0
 */
@Setter
@Getter
public class MultiResponse<T> extends BaseResponse
{
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> datas;
}
