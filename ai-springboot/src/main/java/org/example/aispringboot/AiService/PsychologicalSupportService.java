package org.example.aispringboot.AiService;

import cn.hutool.json.JSONUtil;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PsychologicalSupportService {
    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    @Autowired
    private MoodAnalyzer moodAnalyzer;

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 创建数据库会话记录
        ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);

        // 新建会话:初始情绪基础分 = 50,再叠加首次消息的"偏离度 * 收敛系数 0.6"
        MoodAnalyzer.AnalyzeResult firstShot = moodAnalyzer.analyze(createDTO.getInitialMessage());
        int finalScore = mergeIncrementalScore(50, firstShot.score);
        MoodAnalyzer.AnalyzeResult mood = analyzeFromScore(finalScore);
        String advice = moodAnalyzer.buildAdvice(mood);
        consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), mood.label);
        consultationSessionService.updateLastEmotionAnalysis(consultationSession.getId(), buildMoodJson(mood, advice));

        // 创建会话信息
        String sessionId = "session_" + consultationSession.getId();
        return new StructOutPut.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, // 24小时
                1,
                "ACTIVE"
        );
    }

    public Flux<String> streamPsychologicalChat(String sessionId, String userMessage) {
        // 创建响应流
        return Flux.create(sink->{
            Long dbSessionId = extractSessionId(sessionId);
            if (dbSessionId == null) {
                sink.error(new RuntimeException("会话ID格式错误"));
                return;
            }
            ConsultationSession session = consultationSessionService.getById(dbSessionId);
            if (session == null) {
                sink.error(new RuntimeException("会话不存在"));
                return;
            }

            //判断是否为初始消息
            Boolean isInitialMessage = false;
            Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
            if (messageCount == 1) {
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
                if (lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())) {
                    isInitialMessage = true;
                }
            }

            if (!isInitialMessage) {
                // 读取本会话上一次情绪分数作为基础,再与本轮分析做增量合并
                int prev = getCurrentSessionScore(session);
                MoodAnalyzer.AnalyzeResult thisShot = moodAnalyzer.analyze(userMessage);
                int finalScore = mergeIncrementalScore(prev, thisShot.score);
                MoodAnalyzer.AnalyzeResult mood = analyzeFromScore(finalScore);
                String advice = moodAnalyzer.buildAdvice(mood);
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, mood.label);
                consultationSessionService.updateLastEmotionAnalysis(dbSessionId, buildMoodJson(mood, advice));
            }

            //流式对话
            String conversationId = "conversation_" + sessionId;
            List<Message> userMessages = new ArrayList<>();
            userMessages.add(new UserMessage(userMessage));
            chatMemory.add(conversationId, userMessages);
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT)
            ));

            StringBuilder fullResponse = new StringBuilder();

            chatClient.prompt(prompt)
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(Fragment -> {
                        fullResponse.append(Fragment);
                        sink.next(Fragment);
                    })
                    .doOnComplete(() -> {
                        String completeRes = fullResponse.toString();
                        // 将AI返回的内容保存到数据库
                        consultationMessageService.saveAimessage(dbSessionId, completeRes, "openai");
                        List<Message> aiMessages = new ArrayList<>();
                        aiMessages.add(new AssistantMessage(completeRes));
                        chatMemory.add(conversationId, aiMessages);
                        sink.complete();
                    })
                    .doOnError(error -> sink.error(error))
                    .subscribe();
        });
    }

    // 聚合用户今日情绪(综合今日所有会话的情绪分析,越新权重越高)
    public Map<String, Object> getTodayMood(Long userId) {
        Integer score = consultationSessionService.aggregateTodayScore(userId);
        MoodAnalyzer.AnalyzeResult r;
        String advice;
        if (score == null) {
            // 今日还没有聊天,按中性展示
            r = new MoodAnalyzer.AnalyzeResult(50, "中性", "很不错", 2, "正常");
            advice = "还没有今日的情绪记录,和AI助手聊聊,这里会同步更新你的心情状态哦。";
        } else {
            r = analyzeFromScore(score);
            advice = moodAnalyzer.buildAdvice(r);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("score", r.score);
        map.put("label", r.label);
        map.put("feeling", r.feeling);
        map.put("level", r.level);
        map.put("levelText", r.levelText);
        map.put("advice", advice);
        return map;
    }

    /**
     * 查询某个会话当前的情绪状态(基于 lastEmotionAnalysis)。
     * 如果还没分析过,返回 50 中性初始值 —— 这样新开一个对话就是 50 基线。
     */
    public Map<String, Object> getSessionMood(Long sessionDbId, Long userId) {
        ConsultationSession s = consultationSessionService.getById(sessionDbId);
        if (s == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!s.getUserId().equals(userId)) {
            throw new RuntimeException("无权限访问该会话");
        }
        MoodAnalyzer.AnalyzeResult r;
        String advice;
        Integer prev = parseScoreJson(s.getLastEmotionAnalysis());
        if (prev == null) {
            r = new MoodAnalyzer.AnalyzeResult(50, "中性", "很不错", 2, "正常");
            advice = "该会话暂无情绪记录,发送第一条消息后这里将自动更新。";
        } else {
            r = analyzeFromScore(prev);
            advice = moodAnalyzer.buildAdvice(r);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("score", r.score);
        map.put("label", r.label);
        map.put("feeling", r.feeling);
        map.put("level", r.level);
        map.put("levelText", r.levelText);
        map.put("advice", advice);
        return map;
    }

    /**
     * 会话维度增量记分
     * - 基线 50(中性)
     * - 新消息不会直接覆盖旧分,而是按"偏离基线的部分 * 收敛系数"加到旧分上
     *   这样越聊越趋向稳定,不会因一句就满涨满跌,也不会无限累加超 0-100
     * - 系数:首次 0.6,第2次 0.5,第3次 0.4,第4次及之后 0.3(最低)
     */
    private int mergeIncrementalScore(int prev, int thisShot) {
        int rounds = consultationMessageRoundHolder.incrementAndGet();
        double k = Math.max(0.3, 0.7 - rounds * 0.1);
        int delta = (int) Math.round((thisShot - 50) * k);
        int next = prev + delta;
        return Math.max(0, Math.min(100, next));
    }

    private int getCurrentSessionScore(ConsultationSession s) {
        Integer i = parseScoreJson(s.getLastEmotionAnalysis());
        return i == null ? 50 : i;
    }

    private Integer parseScoreJson(String json) {
        if (cn.hutool.core.util.StrUtil.isBlank(json)) return null;
        try {
            cn.hutool.json.JSONObject jo = cn.hutool.json.JSONUtil.parseObj(json);
            Object s = jo.get("score");
            if (s == null) return null;
            int v = Integer.parseInt(s.toString());
            return Math.max(0, Math.min(100, v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 用来近似"当前已经进行了几轮情绪分析"的内存级近似计数器,
     * 给 mergeIncrementalScore 的 k 系数做递减,避免同一会话多次发送收敛太慢/太快。
     * 不是严格按会话隔离,但效果足够好(重启后重置也ok)。
     */
    private static final java.util.concurrent.atomic.AtomicInteger consultationMessageRoundHolder
            = new java.util.concurrent.atomic.AtomicInteger(0);

    private String buildMoodJson(MoodAnalyzer.AnalyzeResult m, String advice) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("score", m.score);
        map.put("label", m.label);
        map.put("feeling", m.feeling);
        map.put("level", m.level);
        map.put("levelText", m.levelText);
        map.put("advice", advice);
        return JSONUtil.toJsonStr(map);
    }

    private MoodAnalyzer.AnalyzeResult analyzeFromScore(int score) {
        String label, feeling, levelText;
        int level;
        if (score <= 25) {
            label = "低落"; feeling = "不太好"; levelText = "需要关注"; level = 1;
        } else if (score <= 45) {
            label = "偏负面"; feeling = "一般"; levelText = "略偏低"; level = 1;
        } else if (score <= 60) {
            label = "中性"; feeling = "很不错"; levelText = "正常"; level = 2;
        } else if (score <= 80) {
            label = "积极"; feeling = "不错"; levelText = "状态不错"; level = 3;
        } else {
            label = "愉悦"; feeling = "非常好"; levelText = "很理想"; level = 3;
        }
        return new MoodAnalyzer.AnalyzeResult(score, label, feeling, level, levelText);
    }

    // 获取参数中的sessionId
    public Long extractSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("session_")) {
            String idStr = sessionId.substring("session_".length());
            return Long.parseLong(idStr);
        }
        return null;
    }
}