package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.example.aispringboot.mapper.ConsultationSessionMapper;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.example.aispringboot.mapper.UserFavoriteMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端-用户管理服务
 * 提供用户分页查询、详情、历史情绪日记、历史AI咨询记录、启用/禁用账号等能力
 */
@Service
public class AdminUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private ConsultationSessionMapper sessionMapper;

    @Autowired
    private ConsultationMessageMapper messageMapper;

    @Autowired
    private UserFavoriteMapper favoriteMapper;

    /**
     * 分页查询用户列表(支持按用户名/昵称/邮箱模糊搜索)
     * 注意:返回前将密码置空,避免敏感信息泄露
     */
    public Map<String, Object> pageUsers(String keyword, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getNickname, kw)
                    .or().like(User::getEmail, kw)
                    .or().like(User::getPhone, kw));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 脱敏转换:不返回密码字段
        List<Map<String, Object>> records = new ArrayList<>();
        for (User u : result.getRecords()) {
            records.add(toSafeUserMap(u));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    /**
     * 查询用户统计数据(供管理端顶部指标卡片展示)
     */
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("normalUsers", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));
        stats.put("disabledUsers", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 0)));
        stats.put("adminUsers", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUserType, 2)));
        return stats;
    }

    /**
     * 查看用户详情(基本信息 + 情绪日记数/咨询会话数/收藏数)
     */
    public Map<String, Object> getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> data = toSafeUserMap(user);
        data.put("diaryCount", emotionDiaryMapper.selectCount(
                new LambdaQueryWrapper<EmotionDiary>().eq(EmotionDiary::getUserId, userId)));
        data.put("sessionCount", sessionMapper.selectCount(
                new LambdaQueryWrapper<ConsultationSession>().eq(ConsultationSession::getUserId, userId)));
        data.put("favoriteCount", favoriteMapper.selectCount(
                new LambdaQueryWrapper<org.example.aispringboot.entity.UserFavorite>()
                        .eq(org.example.aispringboot.entity.UserFavorite::getUserId, userId)));
        return data;
    }

    /**
     * 查看指定用户的历史情绪日记(按日记日期倒序)
     */
    public List<EmotionDiary> listUserDiaries(Long userId) {
        checkUserExists(userId);
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
                .orderByDesc(EmotionDiary::getDiaryDate)
                .orderByDesc(EmotionDiary::getId);
        return emotionDiaryMapper.selectList(wrapper);
    }

    /**
     * 查看指定用户的历史AI咨询会话列表(按开始时间倒序,附带消息条数)
     */
    public List<Map<String, Object>> listUserSessions(Long userId) {
        checkUserExists(userId);
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getUserId, userId)
                .orderByDesc(ConsultationSession::getStartedAt);
        List<ConsultationSession> sessions = sessionMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ConsultationSession s : sessions) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("sessionTitle", s.getSessionTitle());
            item.put("startedAt", s.getStartedAt());
            item.put("lastEmotionUpdatedAt", s.getLastEmotionUpdatedAt());
            item.put("messageCount", messageMapper.selectCount(
                    new LambdaQueryWrapper<ConsultationMessage>().eq(ConsultationMessage::getSessionId, s.getId())));
            result.add(item);
        }
        return result;
    }

    /**
     * 查看某条咨询会话的全部消息(校验会话归属,防止越权查看)
     */
    public List<ConsultationMessage> listSessionMessages(Long userId, Long sessionId) {
        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("咨询会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("该会话不属于此用户");
        }
        LambdaQueryWrapper<ConsultationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByAsc(ConsultationMessage::getId);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 启用/禁用用户账号
     * 安全约束:不能操作自己的账号;不能禁用其他管理员账号
     *
     * @param currentAdminId 当前登录管理员的用户ID
     * @param targetUserId   目标用户ID
     * @param status         目标状态 0:禁用 1:正常
     */
    public void updateUserStatus(Long currentAdminId, Long targetUserId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("无效的状态参数");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (targetUserId.equals(currentAdminId)) {
            throw new BusinessException("不能操作自己的账号");
        }
        if (status == 0 && Integer.valueOf(2).equals(target.getUserType())) {
            throw new BusinessException("不能禁用管理员账号");
        }
        target.setStatus(status);
        target.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.updateById(target);
    }

    /**
     * 校验用户是否存在
     */
    private void checkUserExists(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
    }

    /**
     * 实体转安全Map(剔除密码,统一供前端展示的字段)
     */
    private Map<String, Object> toSafeUserMap(User u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("nickname", u.getNickname());
        map.put("email", u.getEmail());
        map.put("phone", u.getPhone());
        map.put("gender", u.getGender());
        map.put("birthday", u.getBirthday());
        map.put("userType", u.getUserType());
        map.put("status", u.getStatus());
        map.put("createdAt", u.getCreatedAt());
        map.put("updatedAt", u.getUpdatedAt());
        map.put("lastActiveTime", u.getLastActiveTime());
        return map;
    }
}
