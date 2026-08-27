package org.example.aispringboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 写情绪日记 入参(完全对齐数据库 emotion_diary 表可选写字段)
 */
@Data
public class EmotionDiaryCreateDTO {

    /** 日记日期,不传默认今天 */
    private LocalDate diaryDate;

    /** 情绪评分(1-10,必填) */
    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "情绪评分最低为1")
    @Max(value = 10, message = "情绪评分最高为10")
    private Integer moodScore;

    /** 主要情绪文字:愉快/平静/焦虑/兴奋/孤独...(建议填) */
    @Size(max = 50, message = "主要情绪长度不能超过50")
    private String dominantEmotion;

    /** 情绪触发原因:考试压力/日常学习/感情问题 等 */
    private String emotionTriggers;

    /** 日记正文 */
    private String diaryContent;

    /** 睡眠质量(1-5,可选) */
    @Min(value = 1, message = "睡眠质量最低为1")
    @Max(value = 5, message = "睡眠质量最高为5")
    private Integer sleepQuality;

    /** 压力水平(1-5,可选) */
    @Min(value = 1, message = "压力水平最低为1")
    @Max(value = 5, message = "压力水平最高为5")
    private Integer stressLevel;
}
