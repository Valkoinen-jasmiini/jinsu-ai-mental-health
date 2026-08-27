package org.example.aispringboot.Controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.entity.UserFavorite;
import org.example.aispringboot.entity.UserReadHistory;
import org.example.aispringboot.mapper.*;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private KnowledgeArticleMapper articleMapper;
    
    @Autowired
    private UserFavoriteMapper favoriteMapper;
    
    @Autowired
    private ConsultationSessionMapper sessionMapper;
    
    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private UserReadHistoryMapper readHistoryMapper;

    /**
     * 验证当前用户是否为管理员
     */
    private boolean isAdmin() {
        try {
            // 方法1: 尝试从SecurityContextHolder获取
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Collection authorities = auth.getAuthorities();
                for (Object authority : authorities) {
                    String role = authority.toString();
                    // ROLE_2 表示管理员 (user_type=2)
                    if (role.contains("ROLE_2")) {
                        return true;
                    }
                }
            }
            
            // 方法2: 尝试从JWT token获取
            try {
                String token = JwtTokenUtil.getCurrentToken();
                if (token != null) {
                    DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
                    Integer roleType = jwt.getClaim("roleType").asInt();
                    return roleType != null && roleType == 2;
                }
            } catch (Exception e) {
                // 忽略
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * GET /api/dashboard/stats
     * 获取系统统计数据（仅管理员）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        boolean admin = isAdmin();
        System.out.println("========== Dashboard Stats ==========");
        System.out.println("isAdmin: " + admin);
        
        // 检查认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            System.out.println("Auth principal: " + auth.getPrincipal());
            System.out.println("Auth authorities: " + auth.getAuthorities());
        } else {
            System.out.println("No authentication found!");
        }
        
        if (!admin) {
            System.out.println("Access denied for dashboard stats");
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }

        Map<String, Object> stats = new HashMap<>();
        
        // 总用户数
        Long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);
        
        // 总文章数
        stats.put("totalArticles", articleMapper.selectCount(null));
        
        // 总收藏数
        stats.put("totalFavorites", favoriteMapper.selectCount(null));
        
        // 总会话数
        stats.put("totalSessions", sessionMapper.selectCount(null));
        
        // 总日记数
        stats.put("totalDiaries", emotionDiaryMapper.selectCount(null));

        // 今日新增用户
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        
        Long todayNewUsers = userMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.example.aispringboot.entity.User>()
                .ge("created_at", todayStart)
                .lt("created_at", todayEnd)
        );
        stats.put("todayNewUsers", todayNewUsers);

        // 今日新增会话
        Long todayNewSessions = sessionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ConsultationSession>()
                .ge("started_at", todayStart)
                .lt("started_at", todayEnd)
        );
        stats.put("todayNewSessions", todayNewSessions);

        // 今日新增日记
        Long todayNewDiaries = emotionDiaryMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmotionDiary>()
                .ge("created_at", todayStart)
                .lt("created_at", todayEnd)
        );
        stats.put("todayNewDiaries", todayNewDiaries);

        // 活跃用户（最近7天有活动）
        LocalDateTime sevenDaysAgo = today.minusDays(7).atStartOfDay();
        Long activeUsers = sessionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ConsultationSession>()
                .ge("last_emotion_updated_at", sevenDaysAgo)
        );
        stats.put("activeUsers", activeUsers);
        
        return Result.ok(stats);
    }

    /**
     * GET /api/dashboard/trend
     * 获取30天趋势数据（仅管理员）
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend() {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }

        Map<String, Object> trend = new HashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        // 生成30天的日期标签
        List<String> labels = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            labels.add(today.minusDays(i).format(formatter));
        }
        trend.put("labels", labels);

        // 30天每日新增用户数
        List<Long> userTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            Long count = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.example.aispringboot.entity.User>()
                    .ge("created_at", dayStart)
                    .lt("created_at", dayEnd)
            );
            userTrend.add(count);
        }
        trend.put("userTrend", userTrend);

        // 30天每日新增会话数
        List<Long> sessionTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            Long count = sessionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ConsultationSession>()
                    .ge("started_at", dayStart)
                    .lt("started_at", dayEnd)
            );
            sessionTrend.add(count);
        }
        trend.put("sessionTrend", sessionTrend);

        // 30天每日新增收藏数
        List<Long> favoriteTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            Long count = favoriteMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserFavorite>()
                    .ge("created_at", dayStart)
                    .lt("created_at", dayEnd)
            );
            favoriteTrend.add(count);
        }
        trend.put("favoriteTrend", favoriteTrend);

        // 30天每日日记数
        List<Long> diaryTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            Long count = emotionDiaryMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmotionDiary>()
                    .ge("created_at", dayStart)
                    .lt("created_at", dayEnd)
            );
            diaryTrend.add(count);
        }
        trend.put("diaryTrend", diaryTrend);
        
        return Result.ok(trend);
    }

    /**
     * GET /api/dashboard/my-stats
     * 获取当前用户的个人统计数据
     */
    @GetMapping("/my-stats")
    public Result<Map<String, Object>> getMyStats() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        
        Map<String, Object> stats = new HashMap<>();
        
        // 我的收藏数
        stats.put("myFavorites", favoriteMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserFavorite>()
                .eq("user_id", userId)
        ));
        
        // 我的会话数
        stats.put("mySessions", sessionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ConsultationSession>()
                .eq("user_id", userId)
        ));
        
        // 我的情绪日记数
        stats.put("myDiaries", emotionDiaryMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmotionDiary>()
                .eq("user_id", userId)
        ));
        
        // 我的阅读文章数（去重统计）
        Long readCount = readHistoryMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserReadHistory>()
                .eq("user_id", userId)
        );
        stats.put("myReadArticles", readCount);
        
        return Result.ok(stats);
    }
}
