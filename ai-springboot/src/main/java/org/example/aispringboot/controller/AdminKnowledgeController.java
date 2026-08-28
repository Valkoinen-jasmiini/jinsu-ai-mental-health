package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.aispringboot.DTO.command.KnowledgeArticleUpsertDTO;
import org.example.aispringboot.DTO.command.KnowledgeCategoryUpsertDTO;
import org.example.aispringboot.annotation.OperationLog;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.service.AdminKnowledgeService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 管理端-知识库管理接口(仅管理员可访问)
 * 普通用户端继续使用 /api/knowledge 下的公开接口,互不影响
 */
@RestController
@RequestMapping("/api/knowledge/admin")
public class AdminKnowledgeController {

    @Autowired
    private AdminKnowledgeService adminKnowledgeService;

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
     * 从当前登录token中解析管理员的用户ID(作为文章作者ID)
     */
    private Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    // ==================== 分类管理 ====================

    /**
     * GET /api/knowledge/admin/categories
     * 查询全部分类(含停用)并统计各分类文章数
     */
    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> listCategories() {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminKnowledgeService.listCategoriesWithCount());
    }

    /**
     * POST /api/knowledge/admin/categories
     * 新增分类
     */
    @PostMapping("/categories")
    @OperationLog(module = "知识库管理", operation = "新增分类")
    public Result<String> addCategory(@RequestBody KnowledgeCategoryUpsertDTO dto) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        adminKnowledgeService.addCategory(dto);
        return Result.ok("分类创建成功");
    }

    /**
     * PUT /api/knowledge/admin/categories/{id}
     * 编辑分类
     */
    @PutMapping("/categories/{id}")
    @OperationLog(module = "知识库管理", operation = "编辑分类")
    public Result<String> updateCategory(@PathVariable Long id, @RequestBody KnowledgeCategoryUpsertDTO dto) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        adminKnowledgeService.updateCategory(id, dto);
        return Result.ok("分类更新成功");
    }

    /**
     * DELETE /api/knowledge/admin/categories/{id}
     * 删除分类(分类下有文章或子分类时拒绝)
     */
    @DeleteMapping("/categories/{id}")
    @OperationLog(module = "知识库管理", operation = "删除分类")
    public Result<String> deleteCategory(@PathVariable Long id) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        adminKnowledgeService.deleteCategory(id);
        return Result.ok("分类删除成功");
    }

    // ==================== 文章管理 ====================

    /**
     * GET /api/knowledge/admin/articles?categoryId=&keyword=&page=1&size=10
     * 分页查询全部文章(含下架)
     */
    @GetMapping("/articles")
    public Result<Map<String, Object>> pageArticles(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminKnowledgeService.pageArticles(categoryId, keyword, page, size));
    }

    /**
     * GET /api/knowledge/admin/articles/{id}
     * 文章详情(编辑用,不增加阅读数)
     */
    @GetMapping("/articles/{id}")
    public Result<Map<String, Object>> getArticleDetail(@PathVariable String id) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        return Result.ok(adminKnowledgeService.getArticleDetail(id));
    }

    /**
     * POST /api/knowledge/admin/articles
     * 新增文章(作者为当前管理员)
     */
    @PostMapping("/articles")
    @OperationLog(module = "知识库管理", operation = "新增文章")
    public Result<String> addArticle(@RequestBody KnowledgeArticleUpsertDTO dto) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        adminKnowledgeService.addArticle(dto, getCurrentUserId());
        return Result.ok("文章创建成功");
    }

    /**
     * PUT /api/knowledge/admin/articles/{id}
     * 编辑文章
     */
    @PutMapping("/articles/{id}")
    @OperationLog(module = "知识库管理", operation = "编辑文章")
    public Result<String> updateArticle(@PathVariable String id, @RequestBody KnowledgeArticleUpsertDTO dto) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        adminKnowledgeService.updateArticle(id, dto);
        return Result.ok("文章更新成功");
    }

    /**
     * DELETE /api/knowledge/admin/articles/{id}
     * 删除文章(级联删除收藏与阅读历史)
     */
    @DeleteMapping("/articles/{id}")
    @OperationLog(module = "知识库管理", operation = "删除文章")
    public Result<String> deleteArticle(@PathVariable String id) {
        if (!isAdmin()) {
            return Result.error(ResultCode.ERROR.getCode(), "无权限访问", null);
        }
        adminKnowledgeService.deleteArticle(id);
        return Result.ok("文章删除成功");
    }
}
