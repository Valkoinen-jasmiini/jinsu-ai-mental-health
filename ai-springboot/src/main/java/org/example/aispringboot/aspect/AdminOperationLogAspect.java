package org.example.aispringboot.aspect;

import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.example.aispringboot.annotation.OperationLog;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.AdminOperationLog;
import org.example.aispringboot.mapper.AdminOperationLogMapper;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 管理员操作审计日志切面
 * 拦截标注 @OperationLog 的方法,在业务执行成功(返回code=200)后自动写入审计日志
 * 注意:日志记录失败不影响正常业务
 */
@Aspect
@Component
public class AdminOperationLogAspect {

    /** detail 参数JSON最大保留长度,避免超长内容写库失败 */
    private static final int MAX_DETAIL_LENGTH = 1000;

    @Autowired
    private AdminOperationLogMapper logMapper;

    /**
     * 方法成功返回后记录审计日志(仅记录业务成功 code=200 的操作)
     *
     * @param joinPoint 切点(用于获取方法参数)
     * @param opLog     方法上的注解信息
     * @param result    方法返回值
     */
    @AfterReturning(pointcut = "@annotation(opLog)", returning = "result")
    public void recordLog(JoinPoint joinPoint, OperationLog opLog, Object result) {
        try {
            // 只记录业务成功的操作(无权限/参数校验失败不记录)
            if (!(result instanceof Result)) {
                return;
            }
            if (!isBusinessSuccess((Result<?>) result)) {
                return;
            }

            // 操作人信息(从token解析)
            Long adminId = null;
            String adminUsername = null;
            try {
                String token = JwtTokenUtil.getCurrentToken();
                if (token != null) {
                    DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
                    adminId = jwt.getClaim("userId").asLong();
                    adminUsername = jwt.getClaim("username").asString();
                }
            } catch (Exception ignore) {
                // token解析失败时仍记录日志,操作人留空
            }

            // 操作IP
            String ip = null;
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                ip = attrs.getRequest().getRemoteAddr();
            }

            // 请求参数序列化为JSON(截断到最大长度)
            String detail = null;
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    detail = JSONUtil.toJsonStr(args);
                    if (detail != null && detail.length() > MAX_DETAIL_LENGTH) {
                        detail = detail.substring(0, MAX_DETAIL_LENGTH);
                    }
                }
            } catch (Exception ignore) {
                detail = "参数序列化失败";
            }

            AdminOperationLog log = AdminOperationLog.builder()
                    .adminId(adminId)
                    .adminUsername(adminUsername)
                    .module(opLog.module())
                    .operation(opLog.operation())
                    .detail(detail)
                    .ip(ip)
                    .createdAt(LocalDateTime.now())
                    .build();
            logMapper.insert(log);
        } catch (Exception e) {
            // 审计日志写入失败不影响业务执行
        }
    }

    /**
     * 判断业务返回是否成功(code=200)
     */
    private boolean isBusinessSuccess(Result<?> result) {
        return "200".equals(result.getCode());
    }
}
