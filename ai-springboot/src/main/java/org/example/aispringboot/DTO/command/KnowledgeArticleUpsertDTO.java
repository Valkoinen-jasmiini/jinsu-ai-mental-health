package org.example.aispringboot.DTO.command;

import lombok.Data;

/**
 * 管理端-知识库文章新增/编辑命令DTO
 */
@Data
public class KnowledgeArticleUpsertDTO {

    // 分类ID
    private Long categoryId;

    // 文章标题
    private String title;

    // 摘要
    private String summary;

    // 正文内容(支持 Markdown/纯文本)
    private String content;

    // 封面图片URL
    private String coverImage;

    // 标签(逗号分隔)
    private String tags;

    // 状态 0:下架 1:上架
    private Integer status;
}
