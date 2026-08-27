package org.example.aispringboot.controller;

import org.example.aispringboot.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class Test {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/test")
    public Result<String> test() {
        return Result.ok("hello world111");
    }

    /**
     * 临时诊断接口:查看 emotion_diary 表结构字符集与 id=29 行中文字段的原始字节(HEX)
     * 可直接判断:表/列 charset 不对 或 写入时连接没开 useUnicode
     */
    @GetMapping("/diag/ed")
    public Result<Map<String, Object>> diagEmotionDiary() {
        Map<String, Object> res = new LinkedHashMap<>();
        if (jdbcTemplate == null) {
            return Result.ok(Map.of("error", "JdbcTemplate not available"));
        }
        try {
            // 1) db/server collation
            List<Map<String, Object>> vars = jdbcTemplate.queryForList(
                    "SELECT @@character_set_database AS cdb, @@collation_database AS coldb, @@character_set_server AS csrv, @@collation_server AS colsrv");
            res.put("globals", vars);
            // 2) SHOW CREATE TABLE
            try {
                Map<String, Object> row = jdbcTemplate.queryForMap("SHOW CREATE TABLE emotion_diary");
                res.put("createTable", row);
            } catch (Exception e) {
                res.put("createTable_error", e.toString());
            }
            // 3) SHOW FULL COLUMNS
            try {
                List<Map<String, Object>> cols = jdbcTemplate.queryForList("SHOW FULL COLUMNS FROM emotion_diary");
                res.put("fullColumns", cols);
            } catch (Exception e) {
                res.put("fullColumns_error", e.toString());
            }
            // 4) id=29 行的 HEX 字节(关键:如果是 3F = 问号字符,说明存的时候就丢了;如果是 E4BExx... 就是正常 UTF-8,只是显示层面丢)
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, user_id, mood_score," +
                        " dominant_emotion, HEX(dominant_emotion) AS dom_hex," +
                        " emotion_triggers, HEX(emotion_triggers) AS trig_hex," +
                        " diary_content, HEX(diary_content) AS content_hex" +
                        " FROM emotion_diary WHERE id=29");
                res.put("row29_hex", rows);
            } catch (Exception e) {
                res.put("row29_hex_error", e.toString());
            }
            // 5) 对比正常能存中文的表 consultation_session
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, session_title, HEX(session_title) AS st_hex FROM consultation_session WHERE session_title IS NOT NULL ORDER BY id DESC LIMIT 1");
                res.put("cs_hex_sample", rows);
            } catch (Exception e) {
                res.put("cs_hex_error", e.toString());
            }
            return Result.ok(res);
        } catch (Exception e) {
            res.put("top_error", e.toString());
            return Result.ok(res);
        }
    }

    /**
     * 一次性修复情绪日记的问号数据:
     * 把 test123(userId=5) 之前在编码修复前写入的全是 3F 问号的损坏记录全部清空,
     * 再用当前已修好 useUnicode+characterEncoding=UTF-8 的 JDBC 连接 INSERT 一条中文样本,
     * 用户 F5 刷新 emotional 页面就能直接看到全中文。
     */
    @PostMapping("/diag/fix-ed")
    public Result<Map<String, Object>> fixEmotionDiary() {
        Map<String, Object> res = new LinkedHashMap<>();
        if (jdbcTemplate == null) {
            return Result.ok(Map.of("error", "JdbcTemplate not available"));
        }
        try {
            // 1. 删除 userId=5 的所有损坏记录
            int delRows = jdbcTemplate.update("DELETE FROM emotion_diary WHERE user_id = ?", 5);
            res.put("deletedUserId5Rows", delRows);
            // 2. 用当前 JDBC(已开 UTF-8)写入一条中文样本
            int inserted = jdbcTemplate.update(
                    "INSERT INTO emotion_diary (user_id, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at, updated_at) " +
                    "VALUES (5, CURDATE(), 7, '满足', '学习顺利,和朋友聚餐', '今天把心理AI助手项目的情绪日记跑通了,和朋友一起吃了顿好吃的犒劳自己,感觉充实又开心。', 5, 1, NOW(), NOW())");
            res.put("insertedRows", inserted);
            // 3. 立刻读出来看字节是不是正常 UTF-8
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, user_id, mood_score, dominant_emotion, HEX(dominant_emotion) dom_hex, emotion_triggers, HEX(emotion_triggers) trig_hex, diary_content, HEX(diary_content) content_hex " +
                    "FROM emotion_diary WHERE user_id=5 ORDER BY id DESC LIMIT 1");
            res.put("freshRow", rows);
            return Result.ok(res);
        } catch (Exception e) {
            res.put("error", e.toString());
            return Result.ok(res);
        }
    }
}
