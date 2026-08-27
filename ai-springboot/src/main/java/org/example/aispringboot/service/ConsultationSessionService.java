package org.example.aispringboot.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.mapper.ConsultationSessionMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ConsultationSessionService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConsultationSessionMapper consultationSessionMapper;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 1. 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在，userId: " + userId);
        }

        // 2. 创建会话记录（用户存在时才创建）
        ConsultationSession session = ConsultationSession.builder()
                .userId(userId)
                .sessionTitle(createDTO.getSessionTitle())
                .startedAt(LocalDateTime.now())
                .build();

        // 如果未提供标题，生成默认标题
        if (StrUtil.isBlank(createDTO.getSessionTitle())) {
            session.setSessionTitle(String.format("瑾肃AI助手 - " + DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm")));
        }

        // 3. 插入记录
        int rows = consultationSessionMapper.insert(session);
        if (rows <= 0) {
            throw new RuntimeException("创建会话失败");
        }

        return session;
    }

    /**
     * 查询指定用户的所有会话（按开始时间倒序）
     */
    public List<ConsultationSession> listSessionsByUserId(Long userId) {
        LambdaQueryWrapper<ConsultationSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ConsultationSession::getUserId, userId)
                .orderByDesc(ConsultationSession::getStartedAt);
        return consultationSessionMapper.selectList(qw);
    }

    /**
     * 查询某个会话
     */
    public ConsultationSession getById(Long id) {
        return consultationSessionMapper.selectById(id);
    }

    /**
     * 更新会话最后一次情绪分析(JSON)和时间
     */
    public void updateLastEmotionAnalysis(Long sessionId, String json) {
        LambdaUpdateWrapper<ConsultationSession> uw = new LambdaUpdateWrapper<>();
        uw.eq(ConsultationSession::getId, sessionId)
                .set(ConsultationSession::getLastEmotionAnalysis, json)
                .set(ConsultationSession::getLastEmotionUpdatedAt, LocalDateTime.now());
        consultationSessionMapper.update(null, uw);
    }

    /**
     * 聚合用户今日所有会话最后一次情绪分析:
     * 按更新时间加权(越新权重越高),返回综合score 0-100
     * 如果今天没有任何分析,返回 null
     */
    public Integer aggregateTodayScore(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        LambdaQueryWrapper<ConsultationSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ConsultationSession::getUserId, userId)
                .isNotNull(ConsultationSession::getLastEmotionUpdatedAt)
                .ge(ConsultationSession::getLastEmotionUpdatedAt, startOfDay)
                .le(ConsultationSession::getLastEmotionUpdatedAt, endOfDay);
        List<ConsultationSession> list = consultationSessionMapper.selectList(qw);
        if (list == null || list.isEmpty()) return null;

        // 按 lastEmotionUpdatedAt 最新优先,指数衰减加权:最新 = 1.0,次新 = 0.6,再次 = 0.36...
        List<ConsultationSession> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(ConsultationSession::getLastEmotionUpdatedAt).reversed());

        double weightSum = 0;
        double scoreSum = 0;
        double w = 1.0;
        for (ConsultationSession s : sorted) {
            Integer score = parseScore(s.getLastEmotionAnalysis());
            if (score == null) continue;
            scoreSum += score * w;
            weightSum += w;
            w *= 0.6;
        }
        if (weightSum == 0) return null;
        return (int) Math.round(scoreSum / weightSum);
    }

    /**
     * 从 JSON 中解析 score 字段,解析失败返回 null
     */
    private Integer parseScore(String json) {
        if (StrUtil.isBlank(json)) return null;
        try {
            JSONObject jo = JSONUtil.parseObj(json);
            Object s = jo.get("score");
            if (s == null) return null;
            int v = Integer.parseInt(s.toString());
            return Math.max(0, Math.min(100, v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除会话(只能删当前用户的),级联删除该会话所有消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserAndId(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            throw new IllegalArgumentException("参数错误");
        }
        ConsultationSession exist = consultationSessionMapper.selectById(sessionId);
        if (exist == null) return;
        if (!userId.equals(exist.getUserId())) {
            throw new SecurityException("无权删除他人的会话");
        }
        // 先删消息,再删会话
        consultationMessageService.deleteBySessionId(sessionId);
        consultationSessionMapper.deleteById(sessionId);
    }

    /**
     * 重命名会话(只能改当前用户的)
     */
    public void renameSession(Long userId, Long sessionId, String newTitle) {
        if (userId == null || sessionId == null) {
            throw new IllegalArgumentException("参数错误");
        }
        ConsultationSession exist = consultationSessionMapper.selectById(sessionId);
        if (exist == null) throw new RuntimeException("会话不存在");
        if (!userId.equals(exist.getUserId())) {
            throw new SecurityException("无权修改他人的会话");
        }
        exist.setSessionTitle(newTitle);
        consultationSessionMapper.updateById(exist);
    }
}