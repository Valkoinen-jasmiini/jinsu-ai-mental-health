package org.example.aispringboot.Controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.entity.UserReadHistory;
import org.example.aispringboot.mapper.UserReadHistoryMapper;
import org.example.aispringboot.service.KnowledgeService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private UserReadHistoryMapper readHistoryMapper;

    /**
     * GET /api/knowledge/categories
     * 获取所有启用的分类
     */
    @GetMapping("/categories")
    public Result<List<KnowledgeCategory>> listCategories() {
        return Result.ok(knowledgeService.listCategories());
    }

    /**
     * GET /api/knowledge/articles?categoryId=&keyword=&page=1&size=10
     * 分页查询文章
     */
    @GetMapping("/articles")
    public Result<Map<String, Object>> pageArticles(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(knowledgeService.pageArticles(categoryId, keyword, page, size));
    }

    /**
     * GET /api/knowledge/articles/{id}
     * 文章详情(阅读数+1) + 记录用户阅读历史
     */
    @GetMapping("/articles/{id}")
    public Result<Map<String, Object>> getArticleDetail(@PathVariable String id) {
        // 记录用户阅读历史
        try {
            String token = JwtTokenUtil.getCurrentToken();
            if (token != null) {
                DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
                Long userId = jwt.getClaim("userId").asLong();
                String articleId = id;
                
                // 查询是否已存在阅读记录（同一用户+同一文章）
                Long exists = readHistoryMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserReadHistory>()
                        .eq("user_id", userId)
                        .eq("article_id", articleId)
                );
                // 如果已存在则更新时间，不存在则插入
                if (exists > 0) {
                    UserReadHistory history = readHistoryMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserReadHistory>()
                            .eq("user_id", userId)
                            .eq("article_id", articleId)
                            .last("LIMIT 1")
                    );
                    if (history != null) {
                        history.setCreatedAt(java.time.LocalDateTime.now());
                        readHistoryMapper.updateById(history);
                    }
                } else {
                    UserReadHistory history = new UserReadHistory();
                    history.setUserId(userId);
                    history.setArticleId(articleId);
                    readHistoryMapper.insert(history);
                }
            }
        } catch (Exception e) {
            // 记录失败不影响阅读
        }
        
        return Result.ok(knowledgeService.getArticleDetail(id));
    }

    /**
     * GET /api/knowledge/user/articles/{id}/favorited
     * 检查当前用户是否已收藏
     */
    @GetMapping("/user/articles/{id}/favorited")
    public Result<Boolean> isFavorited(@PathVariable String id) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(knowledgeService.isFavorited(userId, id));
    }

    /**
     * POST /api/knowledge/user/articles/{id}/favorite
     * 收藏文章
     */
    @PostMapping("/user/articles/{id}/favorite")
    public Result<String> favorite(@PathVariable String id) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        knowledgeService.favorite(userId, id);
        return Result.ok("收藏成功");
    }

    /**
     * DELETE /api/knowledge/user/articles/{id}/favorite
     * 取消收藏
     */
    @DeleteMapping("/user/articles/{id}/favorite")
    public Result<String> unfavorite(@PathVariable String id) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        knowledgeService.unfavorite(userId, id);
        return Result.ok("取消收藏成功");
    }

    /**
     * GET /api/knowledge/user/favorites
     * 当前用户的收藏列表
     */
    @GetMapping("/user/favorites")
    public Result<List<Map<String, Object>>> listFavorites() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        return Result.ok(knowledgeService.listFavorites(userId));
    }
}
