package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.aispringboot.annotation.OperationLog;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.service.AdminUserService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 管理端-用户管理接口(仅管理员可访问)
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 验证当前用户是否为管理员(ROLE_2 / token中roleType=2)
     */
    private boolean isAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Collection authorities = auth.getAuthorities();
                for (Object authority : authorities) {
                    if (authority.toString().contains("ROLE_2")) {
                        return true;
                    }
                }
            }
            String token = JwtTokenUtil.getCurrentToken();
            if (token != null) {
                DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
                Integer roleType = jwt.getClaim("roleType").asInt();
                return roleType != null && roleType == 2;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从当前登录token中解析管理员自己的用户ID
     */
    private Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    /**
     * GET /api/admin/user/page?keyword=&page=1&size=10
     * 分页查询用户列表(支持用户名/昵称/邮箱/手机号模糊搜索)
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> pageUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminUserService.pageUsers(keyword, page, size));
    }

    /**
     * GET /api/admin/user/stats
     * 用户统计数据(总用户/正常/禁用/管理员数量)
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminUserService.getUserStats());
    }

    /**
     * GET /api/admin/user/{id}
     * 用户详情(含情绪日记数/咨询会话数/收藏数)
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminUserService.getUserDetail(id));
    }

    /**
     * GET /api/admin/user/{id}/diaries
     * 查看指定用户的历史情绪日记
     */
    @GetMapping("/{id}/diaries")
    public Result<List<EmotionDiary>> getUserDiaries(@PathVariable Long id) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminUserService.listUserDiaries(id));
    }

    /**
     * GET /api/admin/user/{id}/sessions
     * 查看指定用户的历史AI咨询会话列表
     */
    @GetMapping("/{id}/sessions")
    public Result<List<Map<String, Object>>> getUserSessions(@PathVariable Long id) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminUserService.listUserSessions(id));
    }

    /**
     * GET /api/admin/user/{id}/sessions/{sessionId}/messages
     * 查看指定用户某条会话的全部对话消息
     */
    @GetMapping("/{id}/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessage>> getSessionMessages(
            @PathVariable Long id, @PathVariable Long sessionId) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminUserService.listSessionMessages(id, sessionId));
    }

    /**
     * PUT /api/admin/user/{id}/status?status=0|1
     * 启用/禁用用户账号(不能操作自己,不能禁用管理员)
     */
    @PutMapping("/{id}/status")
    @OperationLog(module = "用户管理", operation = "启用/禁用用户账号")
    public Result<String> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        adminUserService.updateUserStatus(getCurrentUserId(), id, status);
        return Result.ok(status == 1 ? "已启用该账号" : "已禁用该账号");
    }
}
