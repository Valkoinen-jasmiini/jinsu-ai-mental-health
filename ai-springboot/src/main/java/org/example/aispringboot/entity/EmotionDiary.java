package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记 Entity,完全对齐用户数据库里的 emotion_diary 表(Navicat设计表字段):
 *
 * 字段清单:
 * id bigint 日记ID(主键自增)
 * user_id bigint 用户ID(非空)
 * diary_date date 日记日期(非空)
 * mood_score tinyint 情绪评分(1-10,越高越好)
 * dominant_emotion varchar(50) 主要情绪(愉快/平静/焦虑/兴奋/孤独 ...)
 * emotion_triggers text 情绪触发因素
 * diary_content longtext 日记内容
 * sleep_quality tinyint 睡眠质量(1-5,可选)
 * stress_level tinyint 压力水平(1-5,可选)
 * ai_emotion_analysis text AI情绪分析结果(JSON格式,可选)
 * ai_analysis_updated_at datetime AI分析更新时间(可选)
 * created_at datetime 创建时间
 * updated_at datetime 更新时间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("emotion_diary")
public class EmotionDiary {

  @TableId(type = IdType.AUTO)
  private Long id;

  @NotNull(message = "用户ID不能为空")
  @TableField("user_id")
  private Long userId;

  @NotNull(message = "日记日期不能为空")
  @TableField("diary_date")
  private LocalDate diaryDate;

  @Min(value = 1, message = "情绪评分最低为1")
  @Max(value = 10, message = "情绪评分最高为10")
  @TableField("mood_score")
  private Integer moodScore;

  @Size(max = 50, message = "主要情绪长度不能超过50")
  @TableField("dominant_emotion")
  private String dominantEmotion;

  @TableField("emotion_triggers")
  private String emotionTriggers;

  @TableField("diary_content")
  private String diaryContent;

  @Min(value = 1, message = "睡眠质量最低为1")
  @Max(value = 5, message = "睡眠质量最高为5")
  @TableField("sleep_quality")
  private Integer sleepQuality;

  @Min(value = 1, message = "压力水平最低为1")
  @Max(value = 5, message = "压力水平最高为5")
  @TableField("stress_level")
  private Integer stressLevel;

  @TableField("ai_emotion_analysis")
  private String aiEmotionAnalysis;

  @TableField("ai_analysis_updated_at")
  private LocalDateTime aiAnalysisUpdatedAt;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;
}
