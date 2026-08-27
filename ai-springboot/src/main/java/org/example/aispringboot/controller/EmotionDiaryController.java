package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.Valid;
import org.example.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.service.EmotionDiaryService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {

    @Autowired
    private EmotionDiaryService emotionDiaryService;

    /**
     * 心情可选字典(10 档 1-10分,以及 5 档 buckets)
     */
    @GetMapping("/options")
    public Result<Map<String, Object>> options() {
        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("levels", emotionDiaryService.moodOptions());
        res.put("buckets", emotionDiaryService.moodBuckets());
        return Result.ok(res);
    }

    /**
     * 写一条日记
     */
    @PostMapping("/add")
    public Result<EmotionDiary> add(@Valid @RequestBody EmotionDiaryCreateDTO dto) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(emotionDiaryService.add(userId, dto));
    }

    /**
     * 某月统计 + 每日聚合 + 每日原始记录
     * @param date 可选,格式 'YYYY-MM',不传默认本月
     */
    @GetMapping("/month")
    public Result<Map<String, Object>> monthStats(@RequestParam(value = "date", required = false) String date) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(emotionDiaryService.monthStats(userId, date));
    }

    /**
     * 今日统计 + 今日所有记录
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> todaySummary() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(emotionDiaryService.todaySummary(userId));
    }

    /**
     * 删除某条日记(只能删自己的)
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id") Long id) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        emotionDiaryService.delete(userId, id);
        return Result.ok("删除成功");
    }
}
