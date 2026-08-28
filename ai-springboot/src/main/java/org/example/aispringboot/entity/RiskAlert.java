package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪风险预警实体
 * 当用户情绪日记触发预警规则(连续低分/高危情绪)时自动生成记录,
 * 供管理端进行主动干预与处理追踪
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("risk_alert")
public class RiskAlert {

    /** 预警ID(自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警用户ID */
    @TableField("user_id")
    private Long userId;

    /** 预警类型: MOOD_LOW-连续低分, HIGH_RISK_EMOTION-高危情绪 */
    @TableField("alert_type")
    private String alertType;

    /** 预警等级: HIGH/MEDIUM/LOW */
    @TableField("alert_level")
    private String alertLevel;

    /** 触发原因说明 */
    @TableField("reason")
    private String reason;

    /** 关联日期(触发当天的日记日期) */
    @TableField("related_date")
    private LocalDate relatedDate;

    /** 处理状态: 0-待处理, 1-已处理 */
    @TableField("status")
    private Integer status;

    /** 处理备注 */
    @TableField("handle_remark")
    private String handleRemark;

    /** 处理人管理员ID */
    @TableField("handled_by")
    private Long handledBy;

    /** 处理时间 */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
