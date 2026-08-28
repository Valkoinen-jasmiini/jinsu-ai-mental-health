package org.example.aispringboot.DTO.command;

import lombok.Data;

/**
 * 管理端-知识库分类新增/编辑命令DTO
 */
@Data
public class KnowledgeCategoryUpsertDTO {

    // 分类名称
    private String categoryName;

    // 分类编码(唯一)
    private String categoryCode;

    // 分类描述
    private String description;

    // 排序号(越小越靠前)
    private Integer sortOrder;

    // 状态 0:停用 1:启用
    private Integer status;
}
