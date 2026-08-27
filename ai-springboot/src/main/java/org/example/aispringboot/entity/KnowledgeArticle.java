package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_article")
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("category_id")
    private Long categoryId;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("content")
    private String content;

    @TableField("cover_image")
    private String coverImage;

    @TableField("tags")
    private String tags;

    @TableField("author_id")
    private Long authorId;

    @TableField("read_count")
    private Integer readCount;

    @TableField("status")
    private Integer status;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
