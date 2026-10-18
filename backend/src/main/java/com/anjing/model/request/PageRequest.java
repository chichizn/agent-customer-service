package com.anjing.model.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页请求类
 * 
 * <p>用于需要分页查询的业务场景</p>
 * 
 * <h3>🎯 设计特点：</h3>
 * <ul>
 *   <li>📄 标准分页 - currentPage和pageSize字段</li>
 *   <li>✅ 参数校验 - 内置合理的参数范围校验</li>
 *   <li>🔧 工具方法 - 提供getOffset()便捷方法</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Setter
@Getter
public class PageRequest extends BaseRequest
{

    /**
     * 当前页（从1开始）
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer currentPage = 1;

    /**
     * 每页结果数
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 1000, message = "每页大小不能超过1000")
    private Integer pageSize = 10;

    /**
     * 获取偏移量（用于数据库查询）
     * 
     * @return 偏移量
     */
    public int getOffset() {
        return (currentPage - 1) * pageSize;
    }

}
