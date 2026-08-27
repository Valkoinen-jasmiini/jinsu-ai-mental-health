package org.example.aispringboot.AiService;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 轻量级情绪分析组件
 * 基于词典 + 否定词反转:对中文文本输出 0-100 分数、情绪标签、建议。
 * 分数含义:0-30 偏低落,31-50 偏中性偏低,51-70 中性偏高,71-100 积极。
 */
@Component
public class MoodAnalyzer {

    // 积极词(+分)
    private static final Map<String, Integer> POSITIVE = new LinkedHashMap<>();
    // 消极词(-分)
    private static final Map<String, Integer> NEGATIVE = new LinkedHashMap<>();
    // 否定/反转前缀
    private static final List<String> NEGATIONS = Arrays.asList(
            "不", "没", "没有", "无", "别", "难以", "无法", "不太", "没那么",
            "并不是", "一点也不", "根本不", "不会", "不要", "不能", "未曾", "绝不"
    );

    static {
        // 积极词 —— 分值越高情绪越强烈
        POSITIVE.put("开心", 10);
        POSITIVE.put("高兴", 10);
        POSITIVE.put("快乐", 10);
        POSITIVE.put("愉快", 9);
        POSITIVE.put("很棒", 10);
        POSITIVE.put("棒", 8);
        POSITIVE.put("太好了", 10);
        POSITIVE.put("太赞了", 10);
        POSITIVE.put("超开心", 12);
        POSITIVE.put("兴奋", 9);
        POSITIVE.put("满足", 8);
        POSITIVE.put("幸福", 12);
        POSITIVE.put("踏实", 6);
        POSITIVE.put("平静", 5);
        POSITIVE.put("安心", 6);
        POSITIVE.put("放松", 6);
        POSITIVE.put("不错", 6);
        POSITIVE.put("挺好", 7);
        POSITIVE.put("顺利", 7);
        POSITIVE.put("成功", 9);
        POSITIVE.put("通过", 6);
        POSITIVE.put("收到offer", 12);
        POSITIVE.put("offer", 10);
        POSITIVE.put("进步", 8);
        POSITIVE.put("喜欢", 7);
        POSITIVE.put("爱", 10);
        POSITIVE.put("感谢", 8);
        POSITIVE.put("感动", 8);
        POSITIVE.put("惊喜", 9);
        POSITIVE.put("乐观", 7);
        POSITIVE.put("自信", 7);
        POSITIVE.put("有希望", 8);

        // 消极词 —— 分值越高越负向
        NEGATIVE.put("难过", 10);
        NEGATIVE.put("伤心", 10);
        NEGATIVE.put("悲伤", 11);
        NEGATIVE.put("哭", 9);
        NEGATIVE.put("想哭", 10);
        NEGATIVE.put("焦虑", 10);
        NEGATIVE.put("焦虑症", 11);
        NEGATIVE.put("不安", 8);
        NEGATIVE.put("担心", 7);
        NEGATIVE.put("害怕", 9);
        NEGATIVE.put("恐惧", 11);
        NEGATIVE.put("紧张", 7);
        NEGATIVE.put("压力大", 10);
        NEGATIVE.put("压力很大", 12);
        NEGATIVE.put("压力", 7);
        NEGATIVE.put("心累", 9);
        NEGATIVE.put("累", 6);
        NEGATIVE.put("疲惫", 8);
        NEGATIVE.put("失眠", 9);
        NEGATIVE.put("睡不着", 9);
        NEGATIVE.put("睡不好", 8);
        NEGATIVE.put("熬夜", 5);
        NEGATIVE.put("emo", 10);
        NEGATIVE.put("抑郁", 12);
        NEGATIVE.put("郁闷", 8);
        NEGATIVE.put("烦躁", 8);
        NEGATIVE.put("烦", 7);
        NEGATIVE.put("烦死", 9);
        NEGATIVE.put("崩溃", 12);
        NEGATIVE.put("崩了", 11);
        NEGATIVE.put("委屈", 8);
        NEGATIVE.put("孤独", 9);
        NEGATIVE.put("无助", 9);
        NEGATIVE.put("绝望", 12);
        NEGATIVE.put("没意思", 8);
        NEGATIVE.put("没意义", 9);
        NEGATIVE.put("无语", 6);
        NEGATIVE.put("生气", 8);
        NEGATIVE.put("愤怒", 10);
        NEGATIVE.put("讨厌", 7);
        NEGATIVE.put("痛恨", 10);
        NEGATIVE.put("想不通", 7);
        NEGATIVE.put("迷茫", 8);
        NEGATIVE.put("低落", 8);
        NEGATIVE.put("糟糕", 9);
        NEGATIVE.put("难受", 8);
        NEGATIVE.put("痛苦", 11);
        NEGATIVE.put("考砸", 9);
        NEGATIVE.put("挂科", 10);
        NEGATIVE.put("被骂", 8);
        NEGATIVE.put("被骂了", 9);
        NEGATIVE.put("分手", 11);
        NEGATIVE.put("失业", 11);
        NEGATIVE.put("找不到工作", 10);
        NEGATIVE.put("面试失败", 9);
        NEGATIVE.put("被拒", 8);
        NEGATIVE.put("失败", 8);
        NEGATIVE.put("空虚", 7);
        NEGATIVE.put("嫉妒", 7);
        NEGATIVE.put("自卑", 8);
        NEGATIVE.put("自我怀疑", 8);
    }

    /**
     * 分析一条文本的情绪
     * @param text 用户输入文本
     * @return { score: 0-100, label:低落/中性/积极... }
     */
    public AnalyzeResult analyze(String text) {
        if (text == null) text = "";
        text = text.trim();
        if (text.isEmpty()) {
            return new AnalyzeResult(50, "中性", 2, "正常");
        }

        int score = 50; // 基础分

        // 1. 正向词累加
        for (Map.Entry<String, Integer> e : POSITIVE.entrySet()) {
            String word = e.getKey();
            int w = e.getValue();
            int idx = 0;
            while ((idx = text.indexOf(word, idx)) != -1) {
                if (hasNegationBefore(text, idx, word.length())) {
                    score = Math.max(0, score - w); // 反转:不开心 ≈ 负向扣分
                } else {
                    score = Math.min(100, score + w);
                }
                idx += word.length();
            }
        }
        // 2. 负向词累加
        for (Map.Entry<String, Integer> e : NEGATIVE.entrySet()) {
            String word = e.getKey();
            int w = e.getValue();
            int idx = 0;
            while ((idx = text.indexOf(word, idx)) != -1) {
                if (hasNegationBefore(text, idx, word.length())) {
                    // "不难过" 减弱负向
                    score = Math.min(100, score + Math.max(1, w / 2));
                } else {
                    score = Math.max(0, score - w);
                }
                idx += word.length();
            }
        }

        // 3. 感叹/问号
        long exclamations = text.chars().filter(c -> c == '!').count();
        score = score + (int) Math.signum(score - 50) * (int) Math.min(10, exclamations * 2);
        long questions = text.chars().filter(c -> c == '?' || c == '？').count();
        if (questions > 0 && score < 50) {
            score = Math.max(0, score - (int) Math.min(8, questions * 2));
        }
        score = Math.max(0, Math.min(100, score));

        // 4. 映射标签/文字
        String label;
        String feeling;
        int level;
        String levelText;
        if (score <= 25) {
            label = "低落";
            feeling = "不太好";
            level = 1;
            levelText = "需要关注";
        } else if (score <= 45) {
            label = "偏负面";
            feeling = "一般";
            level = 1;
            levelText = "略偏低";
        } else if (score <= 60) {
            label = "中性";
            feeling = "很不错";
            level = 2;
            levelText = "正常";
        } else if (score <= 80) {
            label = "积极";
            feeling = "不错";
            level = 3;
            levelText = "状态不错";
        } else {
            label = "愉悦";
            feeling = "非常好";
            level = 3;
            levelText = "很理想";
        }

        return new AnalyzeResult(score, label, feeling, level, levelText);
    }

    /**
     * 生成建议文案
     */
    public String buildAdvice(AnalyzeResult r) {
        int s = r.score;
        if (s <= 25) {
            return "情绪偏低,建议和朋友或家人倾诉,也可以出门散散步、听一段舒缓的音乐;记得,低落是正常的,你不必独自承受。";
        } else if (s <= 45) {
            return "情绪有些起伏,可以试试写下今天让你不舒服的事,拆分它,一点点消化;或者做十分钟深呼吸。";
        } else if (s <= 60) {
            return "情绪状态平稳,继续保持良好的作息;可以做一件让自己小开心的事,比如喝杯喜欢的饮品。";
        } else if (s <= 80) {
            return "状态不错,抓住这份心情去做点有意义的事情吧,也把这份能量传递给身边重要的人。";
        } else {
            return "情绪非常棒!记得记录下今天值得庆祝的小事,留住这份美好的能量 ~";
        }
    }

    private boolean hasNegationBefore(String text, int wordStart, int wordLen) {
        // 在 word 前面 4 个字符窗口内查找否定词
        int from = Math.max(0, wordStart - 6);
        String prefix = text.substring(from, wordStart);
        for (String neg : NEGATIONS) {
            if (prefix.endsWith(neg)) return true;
            if (prefix.contains(neg)) {
                // 更严格:否定词和关键词之间不能间隔过远(>4个字符视为无关)
                int idx = prefix.lastIndexOf(neg);
                if (idx >= 0 && (prefix.length() - idx - neg.length()) <= 3) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class AnalyzeResult {
        public final int score;
        public final String label;
        public final String feeling;
        public final int level;      // 1/2/3 对应红绿灯
        public final String levelText;

        public AnalyzeResult(int score, String label, int level, String levelText) {
            this(score, label, "正常", level, levelText);
        }
        public AnalyzeResult(int score, String label, String feeling, int level, String levelText) {
            this.score = score;
            this.label = label;
            this.feeling = feeling;
            this.level = level;
            this.levelText = levelText;
        }
    }
}
