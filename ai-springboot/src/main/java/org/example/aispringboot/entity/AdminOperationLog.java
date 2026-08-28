package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员操作审计日志实体
 * 通过 @OperationLog 注解 + AOP 切面自动记录管理端敏感操作,实现操作可追溯
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_operation_log")
public class AdminOperationLog {

    /** 日志ID(自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作管理员ID */
    @TableField("admin_id")
    private Long adminId;

    /** 操作管理员账号 */
    @TableField("admin_username")
    private String adminUsername;

    /** 功能模块(用户管理/知识库管理/风险预警) */
    @TableField("module")
    private String module;

    /** 操作类型(如:禁用用户账号) */
    @TableField("operation")
    private String operation;

    /** 操作详情(请求参数JSON) */
    @TableField("detail")
    private String detail;

    /** 操作IP */
    @TableField("ip")
    private String ip;

    /** 操作时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
