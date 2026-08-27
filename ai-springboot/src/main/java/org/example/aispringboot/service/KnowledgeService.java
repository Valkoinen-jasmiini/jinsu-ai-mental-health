package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.entity.KnowledgeArticle;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.entity.UserFavorite;
import org.example.aispringboot.mapper.KnowledgeArticleMapper;
import org.example.aispringboot.mapper.KnowledgeCategoryMapper;
import org.example.aispringboot.mapper.UserFavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    @Autowired
    private KnowledgeCategoryMapper categoryMapper;

    @Autowired
    private KnowledgeArticleMapper articleMapper;

    @Autowired
    private UserFavoriteMapper favoriteMapper;

    /**
     * 获取所有启用的分类(按sort_order升序)
     */
    public List<KnowledgeCategory> listCategories() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1)
               .orderByAsc(KnowledgeCategory::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }

    /**
     * 分页查询文章(可按分类过滤、关键词搜索)
     */
    public Map<String, Object> pageArticles(Long categoryId, String keyword, int page, int size) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, 1);

        if (categoryId != null) {
            wrapper.eq(KnowledgeArticle::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(KnowledgeArticle::getTitle, keyword.trim())
                    .or().like(KnowledgeArticle::getSummary, keyword.trim())
                    .or().like(KnowledgeArticle::getTags, keyword.trim()));
        }
        wrapper.orderByDesc(KnowledgeArticle::getPublishedAt);

        Page<KnowledgeArticle> pageParam = new Page<>(page, size);
        Page<KnowledgeArticle> result = articleMapper.selectPage(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    /**
     * 获取文章详情(含分类名称 + 阅读数+1)
     */
    @Transactional
    public Map<String, Object> getArticleDetail(String articleId) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new org.example.aispringboot.exception.BusinessException("文章不存在");
        }
        // 阅读数+1
        article.setReadCount((article.getReadCount() == null ? 0 : article.getReadCount()) + 1);
        articleMapper.updateById(article);

        Map<String, Object> data = new HashMap<>();
        data.put("article", article);

        // 分类名称
        KnowledgeCategory category = categoryMapper.selectById(article.getCategoryId());
        data.put("categoryName", category != null ? category.getCategoryName() : "");

        return data;
    }

    /**
     * 检查用户是否已收藏某篇文章
     */
    public boolean isFavorited(Long userId, String articleId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .eq(UserFavorite::getArticleId, articleId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    /**
     * 收藏文章
     */
    @Transactional
    public void favorite(Long userId, String articleId) {
        if (isFavorited(userId, articleId)) {
            throw new org.example.aispringboot.exception.BusinessException("已收藏过该文章");
        }
        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId);
        fav.setArticleId(articleId);
        favoriteMapper.insert(fav);
    }

    /**
     * 取消收藏
     */
    @Transactional
    public void unfavorite(Long userId, String articleId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .eq(UserFavorite::getArticleId, articleId);
        favoriteMapper.delete(wrapper);
    }

    /**
     * 获取用户收藏列表(含文章详情)
     */
    public List<Map<String, Object>> listFavorites(Long userId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .orderByDesc(UserFavorite::getCreatedAt);
        List<UserFavorite> favs = favoriteMapper.selectList(wrapper);

        return favs.stream().map(fav -> {
            KnowledgeArticle article = articleMapper.selectById(fav.getArticleId());
            if (article == null) return null;
            Map<String, Object> item = new HashMap<>();
            item.put("articleId", article.getId());
            item.put("title", article.getTitle());
            item.put("summary", article.getSummary());
            item.put("coverImage", article.getCoverImage());
            item.put("tags", article.getTags());
            item.put("publishedAt", article.getPublishedAt());
            item.put("readCount", article.getReadCount());
            // 分类名
            KnowledgeCategory cat = categoryMapper.selectById(article.getCategoryId());
            item.put("categoryName", cat != null ? cat.getCategoryName() : "");
            item.put("favoritedAt", fav.getCreatedAt());
            return item;
        }).filter(item -> item != null).collect(Collectors.toList());
    }
}
