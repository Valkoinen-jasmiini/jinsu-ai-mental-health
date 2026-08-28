package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.DTO.command.KnowledgeArticleUpsertDTO;
import org.example.aispringboot.DTO.command.KnowledgeCategoryUpsertDTO;
import org.example.aispringboot.entity.KnowledgeArticle;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.entity.UserFavorite;
import org.example.aispringboot.entity.UserReadHistory;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.KnowledgeArticleMapper;
import org.example.aispringboot.mapper.KnowledgeCategoryMapper;
import org.example.aispringboot.mapper.UserFavoriteMapper;
import org.example.aispringboot.mapper.UserReadHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理端-知识库管理服务
 * 提供分类管理(增删改查)与文章管理(新增/编辑/删除/上下架)能力
 * 普通用户端的知识库浏览逻辑不受影响(用户端只查 status=1 的数据)
 */
@Service
public class AdminKnowledgeService {

    @Autowired
    private KnowledgeCategoryMapper categoryMapper;

    @Autowired
    private KnowledgeArticleMapper articleMapper;

    @Autowired
    private UserFavoriteMapper favoriteMapper;

    @Autowired
    private UserReadHistoryMapper readHistoryMapper;

    // ==================== 分类管理 ====================

    /**
     * 查询全部分类(含停用的,按sort_order升序),并统计每个分类下的文章数
     */
    public List<Map<String, Object>> listCategoriesWithCount() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(KnowledgeCategory::getSortOrder)
                .orderByAsc(KnowledgeCategory::getId);
        List<KnowledgeCategory> categories = categoryMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgeCategory c : categories) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("parentId", c.getParentId());
            item.put("categoryName", c.getCategoryName());
            item.put("categoryCode", c.getCategoryCode());
            item.put("description", c.getDescription());
            item.put("sortOrder", c.getSortOrder());
            item.put("status", c.getStatus());
            item.put("createdAt", c.getCreatedAt());
            item.put("articleCount", articleMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getCategoryId, c.getId())));
            result.add(item);
        }
        return result;
    }

    /**
     * 新增分类
     */
    public void addCategory(KnowledgeCategoryUpsertDTO dto) {
        validateCategory(dto, null);
        KnowledgeCategory category = new KnowledgeCategory();
        category.setParentId(0L);
        category.setCategoryName(dto.getCategoryName().trim());
        category.setCategoryCode(dto.getCategoryCode() != null ? dto.getCategoryCode().trim() : null);
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
    }

    /**
     * 编辑分类
     */
    public void updateCategory(Long id, KnowledgeCategoryUpsertDTO dto) {
        KnowledgeCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        // 编辑时排除自身ID做编码唯一性校验
        validateCategory(dto, id);
        category.setCategoryName(dto.getCategoryName().trim());
        category.setCategoryCode(dto.getCategoryCode() != null ? dto.getCategoryCode().trim() : null);
        category.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    /**
     * 删除分类(分类下存在文章或存在子分类时不允许删除)
     */
    @Transactional
    public void deleteCategory(Long id) {
        KnowledgeCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getCategoryId, id));
        if (articleCount > 0) {
            throw new BusinessException("该分类下存在 " + articleCount + " 篇文章,请先移除或删除文章");
        }
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeCategory>().eq(KnowledgeCategory::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该分类下存在子分类,无法删除");
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 分类字段校验:名称必填、编码唯一(编辑时排除自身)
     *
     * @param excludeId 编辑时的分类ID(排除自身),新增时传null
     */
    private void validateCategory(KnowledgeCategoryUpsertDTO dto, Long excludeId) {
        if (dto.getCategoryName() == null || dto.getCategoryName().trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (dto.getCategoryCode() != null && !dto.getCategoryCode().trim().isEmpty()) {
            LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeCategory::getCategoryCode, dto.getCategoryCode().trim());
            if (excludeId != null) {
                wrapper.ne(KnowledgeCategory::getId, excludeId);
            }
            if (categoryMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("分类编码已存在: " + dto.getCategoryCode().trim());
            }
        }
    }

    // ==================== 文章管理 ====================

    /**
     * 分页查询全部文章(含下架的,可按分类/关键词过滤),附带分类名称
     */
    public Map<String, Object> pageArticles(Long categoryId, String keyword, int page, int size) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(KnowledgeArticle::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(KnowledgeArticle::getTitle, kw)
                    .or().like(KnowledgeArticle::getSummary, kw)
                    .or().like(KnowledgeArticle::getTags, kw));
        }
        wrapper.orderByDesc(KnowledgeArticle::getCreatedAt);

        Page<KnowledgeArticle> result = articleMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (KnowledgeArticle a : result.getRecords()) {
            records.add(toArticleMap(a));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    /**
     * 查看文章详情(编辑用,不增加阅读数)
     */
    public Map<String, Object> getArticleDetail(String articleId) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        return toArticleMap(article);
    }

    /**
     * 新增文章(ID使用UUID,与现有知识库文章ID格式保持一致)
     */
    public void addArticle(KnowledgeArticleUpsertDTO dto, Long authorId) {
        validateArticle(dto);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(UUID.randomUUID().toString());
        article.setCategoryId(dto.getCategoryId());
        article.setTitle(dto.getTitle().trim());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCoverImage(dto.getCoverImage());
        article.setTags(dto.getTags());
        article.setAuthorId(authorId);
        article.setReadCount(0);
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        // 上架状态记录发布时间
        if (article.getStatus() != null && article.getStatus() == 1) {
            article.setPublishedAt(LocalDateTime.now());
        }
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.insert(article);
    }

    /**
     * 编辑文章
     */
    public void updateArticle(String articleId, KnowledgeArticleUpsertDTO dto) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        validateArticle(dto);
        article.setCategoryId(dto.getCategoryId());
        article.setTitle(dto.getTitle().trim());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCoverImage(dto.getCoverImage());
        article.setTags(dto.getTags());
        if (dto.getStatus() != null) {
            article.setStatus(dto.getStatus());
            // 首次上架时补记发布时间
            if (dto.getStatus() == 1 && article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
        }
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    /**
     * 删除文章(级联删除该文章的收藏记录与阅读历史,保证引用完整性)
     */
    @Transactional
    public void deleteArticle(String articleId) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        favoriteMapper.delete(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getArticleId, articleId));
        readHistoryMapper.delete(new LambdaQueryWrapper<UserReadHistory>()
                .eq(UserReadHistory::getArticleId, articleId));
        articleMapper.deleteById(articleId);
    }

    /**
     * 文章字段校验:标题必填、分类必须存在
     */
    private void validateArticle(KnowledgeArticleUpsertDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException("文章标题不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException("请选择文章分类");
        }
        if (categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BusinessException("所选分类不存在");
        }
    }

    /**
     * 文章实体转Map(附带分类名称,统一供前端展示)
     */
    private Map<String, Object> toArticleMap(KnowledgeArticle a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("categoryId", a.getCategoryId());
        map.put("title", a.getTitle());
        map.put("summary", a.getSummary());
        map.put("content", a.getContent());
        map.put("coverImage", a.getCoverImage());
        map.put("tags", a.getTags());
        map.put("authorId", a.getAuthorId());
        map.put("readCount", a.getReadCount());
        map.put("status", a.getStatus());
        map.put("publishedAt", a.getPublishedAt());
        map.put("createdAt", a.getCreatedAt());
        map.put("updatedAt", a.getUpdatedAt());
        KnowledgeCategory cat = categoryMapper.selectById(a.getCategoryId());
        map.put("categoryName", cat != null ? cat.getCategoryName() : "");
        return map;
    }
}
