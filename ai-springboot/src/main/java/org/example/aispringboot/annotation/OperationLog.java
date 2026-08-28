package org.example.aispringboot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理员操作审计日志注解
 * 标注在管理端 Controller 方法上,由 AdminOperationLogAspect 切面自动记录操作日志
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 功能模块(如:用户管理/知识库管理/风险预警)
     */
    String module();

    /**
     * 操作类型(如:禁用用户账号/删除文章)
     */
    String operation();
}
