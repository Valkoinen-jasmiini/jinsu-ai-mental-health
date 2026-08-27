package org.example.aispringboot.controller;

import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.Valid;
import org.example.aispringboot.AiService.PsychologicalSupportService;
import org.example.aispringboot.AiService.StructOutPut;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.command.ConsultationStreamDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChat {
    @Autowired
    private PsychologicalSupportService psychologicalSupportService;

    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;


    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid@RequestBody ConsultationSessionCreateDTO createDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
        return Result.ok(session);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(),"用户未登录")))
                    .build());
        }

        //开始流式对话
        return psychologicalSupportService.streamPsychologicalChat(streamDTO.getSessionId(), streamDTO.getUserMessage())
                .map(Fragment->{
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(JSONUtil.toJsonStr(Result.ok(Map.of("content",Fragment,"type","normal"))))
                            .build();

                })
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()
                ))
                .delayElements(Duration.ofMillis(50));// 每个事件之间延迟50毫秒,添加延迟确保流式输出的顺序性
    }

    /**
     * 查询当前用户的会话列表
     */
    @GetMapping("/sessions")
    public Result<List<ConsultationSession>> listSessions() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(consultationSessionService.listSessionsByUserId(userId));
    }

    /**
     * 查询某个会话的消息历史
     */
    @GetMapping("/sessions/{sessionDbId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> listMessages(@PathVariable("sessionDbId") Long sessionDbId) {
        // 简单校验：确保会话属于当前用户，避免越权
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        // 先按 id 查会话，校验 userId
        return Result.ok(consultationMessageService.listMessagesBySessionId(sessionDbId));
    }

    /**
     * 查询当前用户今日情绪概要:综合今日所有会话最后一次情绪分析(越新权重越高)
     * 返回: score(0-100) label(低落/偏负面/中性/积极/愉悦) feeling level(1-3) levelText advice
     */
    @GetMapping("/mood/today")
    public Result<Map<String, Object>> getTodayMood() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(psychologicalSupportService.getTodayMood(userId));
    }

    /**
     * 查询某个会话当前的情绪状态(按会话维度独立记分,新开会话初始为50中性)
     */
    @GetMapping("/mood/session/{sessionDbId}")
    public Result<Map<String, Object>> getSessionMood(@PathVariable("sessionDbId") Long sessionDbId) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(psychologicalSupportService.getSessionMood(sessionDbId, userId));
    }

    /**
     * 删除某个会话(级联删除该会话所有消息,只能删当前登录用户自己的)
     */
    @DeleteMapping("/sessions/{sessionDbId}")
    public Result<String> deleteSession(@PathVariable("sessionDbId") Long sessionDbId) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        consultationSessionService.deleteByUserAndId(userId, sessionDbId);
        return Result.ok("删除成功");
    }

    /**
     * 重命名会话
     */
    @PutMapping("/sessions/{sessionDbId}/rename")
    public Result<String> renameSession(@PathVariable("sessionDbId") Long sessionDbId, 
                                         @RequestBody Map<String, String> body) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        String newTitle = body.get("sessionTitle");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return Result.error(ResultCode.ERROR.getCode(), "标题不能为空", null);
        }
        consultationSessionService.renameSession(userId, sessionDbId, newTitle.trim());
        return Result.ok("重命名成功");
    }
}
