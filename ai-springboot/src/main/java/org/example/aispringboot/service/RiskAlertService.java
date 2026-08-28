package org.example.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.entity.RiskAlert;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.example.aispringboot.mapper.RiskAlertMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 情绪风险预警服务
 * 触发规则(在情绪日记提交后自动检查):
 * 1. HIGH_RISK_EMOTION: 日记的情绪/触发因素/内容命中高危关键词(如抑郁、自伤等)
 * 2. MOOD_LOW: 最近3条不同日期的日记情绪评分均 <= 3(连续低落)
 * 去重策略: 同一用户同一类型存在未处理预警,或当天已生成过同类型预警时不再重复生成
 */
@Service
public class RiskAlertService {

    /** 高危情绪关键词(命中即生成高危预警) */
    private static final String[] HIGH_RISK_KEYWORDS = {
            "自杀", "自残", "自伤", "轻生", "想死", "不想活", "伤害自己", "活不下去"
    };

    /** 连续低分判定阈值:最近3条日记均 <= 该分值(1-10分制)则预警 */
    private static final int LOW_SCORE_THRESHOLD = 3;

    /** 连续低分判定的日记条数窗口 */
    private static final int LOW_SCORE_WINDOW = 3;

    @Autowired
    private RiskAlertMapper riskAlertMapper;

    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private UserMapper userMapper;

    // ==================== 预警触发(日记提交后调用) ====================

    /**
     * 情绪日记提交后的风险检查入口
     * 任何异常都不影响日记正常提交(检查失败仅跳过本次预警)
     */
    public void checkDiaryRisk(EmotionDiary diary) {
        try {
            if (diary == null || diary.getUserId() == null) {
                return;
            }
            // 规则1:高危情绪词检查(优先级高)
            checkHighRiskEmotion(diary);
            // 规则2:连续低分检查
            checkContinuousLowScore(diary);
        } catch (Exception e) {
            // 预警检查失败不影响日记提交
        }
    }

    /**
     * 规则1:日记文本命中高危关键词 -> 生成 HIGH_RISK_EMOTION 预警
     */
    private void checkHighRiskEmotion(EmotionDiary diary) {
        String text = StrUtil.nullToEmpty(diary.getDominantEmotion())
                + " " + StrUtil.nullToEmpty(diary.getEmotionTriggers())
                + " " + StrUtil.nullToEmpty(diary.getDiaryContent());
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (text.contains(keyword)) {
                String reason = "日记内容命中高危情绪关键词「" + keyword + "」";
                createAlertIfAbsent(diary.getUserId(), "HIGH_RISK_EMOTION", "HIGH",
                        reason, diary.getDiaryDate());
                return;
            }
        }
    }

    /**
     * 规则2:最近3条不同日期日记评分均<=3 -> 生成 MOOD_LOW 预警
     */
    private void checkContinuousLowScore(EmotionDiary diary) {
        if (diary.getMoodScore() == null || diary.getMoodScore() > LOW_SCORE_THRESHOLD) {
            return;
        }
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, diary.getUserId())
                .orderByDesc(EmotionDiary::getDiaryDate)
                .orderByDesc(EmotionDiary::getId)
                .last("LIMIT " + LOW_SCORE_WINDOW);
        List<EmotionDiary> recent = emotionDiaryMapper.selectList(wrapper);
        // 窗口不足3条不判定,全部低分才触发
        if (recent.size() < LOW_SCORE_WINDOW) {
            return;
        }
        boolean allLow = recent.stream()
                .allMatch(d -> d.getMoodScore() != null && d.getMoodScore() <= LOW_SCORE_THRESHOLD);
        if (allLow) {
            String reason = "最近 " + LOW_SCORE_WINDOW + " 天情绪评分均低于等于 " + LOW_SCORE_THRESHOLD
                    + " 分,存在持续情绪低落风险";
            createAlertIfAbsent(diary.getUserId(), "MOOD_LOW", "MEDIUM",
                    reason, diary.getDiaryDate());
        }
    }

    /**
     * 生成预警(带去重):
     * 1. 同用户同类型存在"待处理"预警时不重复生成
     * 2. 同用户同类型同关联日期已生成过时不再生成
     */
    private void createAlertIfAbsent(Long userId, String alertType, String alertLevel,
                                     String reason, LocalDate relatedDate) {
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getUserId, userId)
                .eq(RiskAlert::getAlertType, alertType)
                .eq(RiskAlert::getRelatedDate, relatedDate);
        if (riskAlertMapper.selectCount(wrapper) > 0) {
            return;
        }
        LambdaQueryWrapper<RiskAlert> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(RiskAlert::getUserId, userId)
                .eq(RiskAlert::getAlertType, alertType)
                .eq(RiskAlert::getStatus, 0);
        if (riskAlertMapper.selectCount(pendingWrapper) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        RiskAlert alert = RiskAlert.builder()
                .userId(userId)
                .alertType(alertType)
                .alertLevel(alertLevel)
                .reason(reason)
                .relatedDate(relatedDate)
                .status(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        riskAlertMapper.insert(alert);
    }

    // ==================== 管理端查询与处理 ====================

    /**
     * 分页查询预警列表(附带用户账号/昵称,支持按处理状态筛选)
     */
    public Map<String, Object> pageAlerts(Integer status, int page, int size) {
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RiskAlert::getStatus, status);
        }
        wrapper.orderByAsc(RiskAlert::getStatus)
                .orderByDesc(RiskAlert::getCreatedAt);
        Page<RiskAlert> result = riskAlertMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (RiskAlert a : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("userId", a.getUserId());
            item.put("alertType", a.getAlertType());
            item.put("alertLevel", a.getAlertLevel());
            item.put("reason", a.getReason());
            item.put("relatedDate", a.getRelatedDate());
            item.put("status", a.getStatus());
            item.put("handleRemark", a.getHandleRemark());
            item.put("handledAt", a.getHandledAt());
            item.put("createdAt", a.getCreatedAt());
            User user = userMapper.selectById(a.getUserId());
            item.put("username", user != null ? user.getUsername() : "已删除用户");
            item.put("nickname", user != null ? user.getNickname() : "");
            records.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    /**
     * 预警统计数据:总数/待处理/已处理/今日新增
     */
    public Map<String, Object> alertStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", riskAlertMapper.selectCount(null));
        stats.put("pending", riskAlertMapper.selectCount(
                new LambdaQueryWrapper<RiskAlert>().eq(RiskAlert::getStatus, 0)));
        stats.put("handled", riskAlertMapper.selectCount(
                new LambdaQueryWrapper<RiskAlert>().eq(RiskAlert::getStatus, 1)));
        stats.put("todayNew", riskAlertMapper.selectCount(
                new LambdaQueryWrapper<RiskAlert>()
                        .ge(RiskAlert::getCreatedAt, LocalDate.now().atStartOfDay())));
        return stats;
    }

    /**
     * 处理预警:标记为已处理并记录处理人与备注
     */
    public void handleAlert(Long alertId, String remark, Long adminId) {
        RiskAlert alert = riskAlertMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }
        if (alert.getStatus() == 1) {
            throw new BusinessException("该预警已处理过");
        }
        alert.setStatus(1);
        alert.setHandleRemark(StrUtil.isBlank(remark) ? "已回访处理" : remark.trim());
        alert.setHandledBy(adminId);
        alert.setHandledAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());
        riskAlertMapper.updateById(alert);
    }
}
