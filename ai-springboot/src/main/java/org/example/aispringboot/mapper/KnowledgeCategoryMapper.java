package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {
}
