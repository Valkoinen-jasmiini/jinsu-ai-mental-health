package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.AdminOperationLog;
import org.example.aispringboot.mapper.AdminOperationLogMapper;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端-操作审计日志查询接口(仅管理员可访问)
 * 日志数据由 @OperationLog 注解 + AOP 切面自动写入
 */
@RestController
@RequestMapping("/api/admin/log")
public class AdminLogController {

    @Autowired
    private AdminOperationLogMapper logMapper;

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
     * GET /api/admin/log/page?module=&keyword=&page=1&size=10
     * 分页查询操作日志(可按模块筛选,keyword模糊匹配操作类型/详情/操作人)
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> pageLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.trim().isEmpty()) {
            wrapper.eq(AdminOperationLog::getModule, module.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AdminOperationLog::getOperation, kw)
                    .or().like(AdminOperationLog::getDetail, kw)
                    .or().like(AdminOperationLog::getAdminUsername, kw));
        }
        wrapper.orderByDesc(AdminOperationLog::getCreatedAt);

        Page<AdminOperationLog> result = logMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.ok(data);
    }
}
