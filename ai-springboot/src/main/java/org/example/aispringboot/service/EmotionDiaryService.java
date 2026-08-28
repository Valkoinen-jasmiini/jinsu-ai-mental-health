package org.example.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 情绪日记 Service —— 操作数据库真实表 emotion_diary
 *
 * 关于分数:
 * - 数据库存 1-10(mood_score, tinyint)
 * - 输出给前端时,统一 ×10 映射成 0-100 的"显示分"以便和前端通用风格对齐
 * 前端如果需要显示 1-10,再做 /10 也ok
 * - 记录插入:如果没填 dominantEmotion,后端根据 moodScore 兜底生成"很差/低落/一般/不错/开心"
 */
@Service
public class EmotionDiaryService {

    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    @Autowired
    private UserMapper userMapper;

    // 情绪风险预警服务(日记提交后自动检查是否触发预警)
    @Autowired
    private RiskAlertService riskAlertService;

    // 1-10 分 → 中文兜底标签 + 颜色
    // 1-2 很差, 3-4 低落, 5-6 一般, 7-8 不错, 9-10 开心
    private static final int[][] SCORE_RANGES = {
            { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 }, { 9, 10 }
    };
    private static final String[] DEFAULT_LABELS = { "很差", "低落", "一般", "不错", "开心" };
    private static final String[] DEFAULT_COLORS = { "#ef4444", "#f97316", "#eab308", "#84cc16", "#22c55e" };

    // ===== 字典:前端渲染心情选项 =====
    // 这里给前端导出 10 个等级(1-10),label 5 档 + emoji
    public List<Map<String, Object>> moodOptions() {
        // 10 档:每个分对应一行,颜色按档位出
        List<Map<String, Object>> list = new ArrayList<>();
        int[] moodScoreDefaults = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        String[] defaultEmojis = { "😫", "😣", "😔", "😕", "😐", "🙂", "🙂", "😊", "😄", "🥳" };
        for (int i = 0; i < moodScoreDefaults.length; i++) {
            int s = moodScoreDefaults[i];
            int bucket = bucketOf(s);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("score", s); // 1-10,真实存库分
            row.put("displayScore", s * 10); // 0-100 UI展示用
            row.put("label", DEFAULT_LABELS[bucket]);
            row.put("color", DEFAULT_COLORS[bucket]);
            row.put("emoji", defaultEmojis[i]);
            list.add(row);
        }
        return list;
    }

    /**
     * 5 档心情:给日历"心情圆点/最多心情"这种分类用。
     */
    public List<Map<String, Object>> moodBuckets() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < DEFAULT_LABELS.length; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", i);
            row.put("label", DEFAULT_LABELS[i]);
            row.put("color", DEFAULT_COLORS[i]);
            list.add(row);
        }
        return list;
    }

    /**
     * 新增一条日记
     */
    public EmotionDiary add(Long userId, EmotionDiaryCreateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null)
            throw new RuntimeException("用户不存在");

        LocalDate diaryDate = dto.getDiaryDate() != null ? dto.getDiaryDate() : LocalDate.now();
        Integer moodScore = normalize(dto.getMoodScore(), 1, 10, 6);

        String dominantEmotion = dto.getDominantEmotion();
        if (StrUtil.isBlank(dominantEmotion)) {
            dominantEmotion = DEFAULT_LABELS[bucketOf(moodScore)];
        }

        LocalDateTime now = LocalDateTime.now();
        EmotionDiary entity = EmotionDiary.builder()
                .userId(userId)
                .diaryDate(diaryDate)
                .moodScore(moodScore)
                .dominantEmotion(dominantEmotion)
                .emotionTriggers(dto.getEmotionTriggers())
                .diaryContent(dto.getDiaryContent())
                .sleepQuality(dto.getSleepQuality())
                .stressLevel(dto.getStressLevel())
                .createdAt(now)
                .updatedAt(now)
                .build();

        emotionDiaryMapper.insert(entity);

        // 提交后进行情绪风险检查(连续低分/高危情绪词),异常不影响日记提交
        riskAlertService.checkDiaryRisk(entity);

        return entity;
    }

    /**
     * 某月全部记录(按日期+创建时间升序)
     */
    public List<EmotionDiary> listByMonth(Long userId, String yyyyMM) {
        YearMonth ym = parseYearMonth(yyyyMM);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        LambdaQueryWrapper<EmotionDiary> qw = new LambdaQueryWrapper<>();
        qw.eq(EmotionDiary::getUserId, userId)
                .ge(EmotionDiary::getDiaryDate, from)
                .le(EmotionDiary::getDiaryDate, to)
                .orderByAsc(EmotionDiary::getDiaryDate, EmotionDiary::getCreatedAt);
        return emotionDiaryMapper.selectList(qw);
    }

    /**
     * 某月统计
     */
    public Map<String, Object> monthStats(Long userId, String yyyyMM) {
        List<EmotionDiary> list = listByMonth(userId, yyyyMM);
        Map<String, Object> res = new LinkedHashMap<>();
        int count = list.size();
        res.put("recordCount", count);
        if (count == 0) {
            res.put("avgScore", null); // 0-100
            res.put("avgMoodScore10", null); // 1-10
            res.put("topLabel", "暂无记录");
            res.put("topColor", "#9ca3af");
            res.put("days", Collections.emptyMap());
            res.put("logsByDate", Collections.emptyMap());
            return res;
        }

        int sum = 0;
        // 主情绪频次(用 dominant_emotion 文本本身作为 bucket)
        Map<String, Long> labelFreq = new HashMap<>();
        for (EmotionDiary d : list) {
            sum += (d.getMoodScore() == null ? 6 : d.getMoodScore());
            String label = StrUtil.isBlank(d.getDominantEmotion())
                    ? DEFAULT_LABELS[bucketOf(d.getMoodScore() == null ? 6 : d.getMoodScore())]
                    : d.getDominantEmotion();
            labelFreq.merge(label, 1L, Long::sum);
        }
        double avg10 = (double) sum / count; // 1-10
        int avg100 = (int) Math.round(avg10 * 10); // 0-100
        String topLabel = Collections.max(labelFreq.entrySet(), Comparator.comparingLong(Map.Entry::getValue)).getKey();
        String topColor = colorForLabel(topLabel, (int) Math.round(avg10));

        res.put("avgScore", avg100);
        res.put("avgMoodScore10", round1(avg10));
        res.put("topLabel", topLabel);
        res.put("topColor", topColor);

        // 按天聚合:每天平均分(100) + 当天主情绪 + 颜色
        Map<String, DaySummary> daySummaryMap = list.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDiaryDate().toString(),
                        Collectors.collectingAndThen(Collectors.toList(), lst -> {
                            int s = lst.stream()
                                    .mapToInt(d -> d.getMoodScore() == null ? 6 : d.getMoodScore())
                                    .sum();
                            double a10 = (double) s / lst.size();
                            int a100 = (int) Math.round(a10 * 10);
                            Map<String, Long> f = lst.stream()
                                    .collect(Collectors.groupingBy(
                                            d -> StrUtil.isBlank(d.getDominantEmotion())
                                                    ? DEFAULT_LABELS[bucketOf(
                                                            d.getMoodScore() == null ? 6 : d.getMoodScore())]
                                                    : d.getDominantEmotion(),
                                            Collectors.counting()));
                            String top = Collections.max(f.entrySet(), Comparator.comparingLong(Map.Entry::getValue))
                                    .getKey();
                            String c = colorForLabel(top, (int) Math.round(a10));
                            return new DaySummary(a100, top, c);
                        })));
        res.put("days", daySummaryMap);

        // 按天原始记录(右侧详情用)
        Map<String, List<EmotionDiary>> byDate = list.stream()
                .collect(Collectors.groupingBy(d -> d.getDiaryDate().toString(), LinkedHashMap::new,
                        Collectors.toList()));
        res.put("logsByDate", byDate);
        return res;
    }

    /**
     * 今日统计 + 今日所有记录
     */
    public Map<String, Object> todaySummary(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<EmotionDiary> qw = new LambdaQueryWrapper<>();
        qw.eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, today)
                .orderByDesc(EmotionDiary::getCreatedAt);
        List<EmotionDiary> list = emotionDiaryMapper.selectList(qw);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("date", today.toString());
        res.put("recordCount", list.size());
        if (list.isEmpty()) {
            res.put("avgScore", null);
            res.put("avgMoodScore10", null);
            res.put("topLabel", "未记录");
            res.put("topColor", "#9ca3af");
            res.put("logs", Collections.emptyList());
        } else {
            int s = list.stream().mapToInt(d -> d.getMoodScore() == null ? 6 : d.getMoodScore()).sum();
            double a10 = (double) s / list.size();
            int a100 = (int) Math.round(a10 * 10);
            Map<String, Long> freq = list.stream()
                    .collect(Collectors.groupingBy(
                            d -> StrUtil.isBlank(d.getDominantEmotion())
                                    ? DEFAULT_LABELS[bucketOf(d.getMoodScore() == null ? 6 : d.getMoodScore())]
                                    : d.getDominantEmotion(),
                            Collectors.counting()));
            String top = Collections.max(freq.entrySet(), Comparator.comparingLong(Map.Entry::getValue)).getKey();
            String c = colorForLabel(top, (int) Math.round(a10));
            res.put("avgScore", a100);
            res.put("avgMoodScore10", round1(a10));
            res.put("topLabel", top);
            res.put("topColor", c);
            res.put("logs", list);
        }
        return res;
    }

    // ===== 辅助 =====
    private int bucketOf(int moodScore10) {
        for (int i = 0; i < SCORE_RANGES.length; i++) {
            if (moodScore10 >= SCORE_RANGES[i][0] && moodScore10 <= SCORE_RANGES[i][1])
                return i;
        }
        return 2; // 默认一般
    }

    private String colorForLabel(String label, int moodScore10) {
        // 优先看 label 能不能命中历史数据里常见的积极/消极词
        if (StrUtil.isBlank(label)) {
            return DEFAULT_COLORS[bucketOf(moodScore10)];
        }
        String low = label.toLowerCase();
        // 先看明显词汇
        if (Arrays.asList("开心", "愉快", "兴奋", "充实", "坚定", "好奇", "满足", "幸福", "惊喜", "愉悦", "高兴")
                .contains(label))
            return "#22c55e";
        if (Arrays.asList("平静", "轻松", "安静", "平和").contains(label))
            return "#84cc16";
        if (Arrays.asList("焦虑", "低落", "孤独", "委屈", "疲惫", "压力", "挫折", "烦恼", "不安", "生气", "郁闷", "迷茫", "担心", "紧张")
                .contains(label))
            return "#f97316";
        // 否则按 1-10 分兜底
        return DEFAULT_COLORS[bucketOf(moodScore10)];
    }

    private Integer normalize(Integer v, int min, int max, int dflt) {
        if (v == null)
            return dflt;
        return Math.max(min, Math.min(max, v));
    }

    private YearMonth parseYearMonth(String yyyyMM) {
        if (StrUtil.isBlank(yyyyMM))
            return YearMonth.now();
        try {
            return YearMonth.parse(yyyyMM.trim());
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 删除某条日记(只能删自己的)
     */
    public void delete(Long userId, Long id) {
        EmotionDiary exist = emotionDiaryMapper.selectById(id);
        if (exist == null)
            return;
        if (!userId.equals(exist.getUserId())) {
            throw new IllegalArgumentException("无权删除他人的日记");
        }
        emotionDiaryMapper.deleteById(id);
    }

    public static class DaySummary {
        public final int avgScore; // 0-100
        public final String moodLabel;
        public final String moodColor;

        public DaySummary(int avgScore, String moodLabel, String moodColor) {
            this.avgScore = avgScore;
            this.moodLabel = moodLabel;
            this.moodColor = moodColor;
        }
    }
}
