package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.aispringboot.annotation.OperationLog;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.service.RiskAlertService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * 管理端-情绪风险预警接口(仅管理员可访问)
 * 预警记录由用户提交情绪日记时自动生成,管理员在此查看并处理
 */
@RestController
@RequestMapping("/api/admin/alert")
public class AdminRiskAlertController {

    @Autowired
    private RiskAlertService riskAlertService;

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
     * 从当前登录token中解析管理员自己的用户ID(作为预警处理人)
     */
    private Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    /**
     * GET /api/admin/alert/page?status=&page=1&size=10
     * 分页查询预警列表(status为空查全部,待处理排前)
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> pageAlerts(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(riskAlertService.pageAlerts(status, page, size));
    }

    /**
     * GET /api/admin/alert/stats
     * 预警统计(总数/待处理/已处理/今日新增)
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> alertStats() {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(riskAlertService.alertStats());
    }

    /**
     * PUT /api/admin/alert/{id}/handle?remark=xxx
     * 处理预警(标记已处理并记录处理人与备注)
     */
    @PutMapping("/{id}/handle")
    @OperationLog(module = "风险预警", operation = "处理风险预警")
    public Result<String> handleAlert(@PathVariable Long id,
                                      @RequestParam(required = false) String remark) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        riskAlertService.handleAlert(id, remark, getCurrentUserId());
        return Result.ok("预警已处理");
    }
}
