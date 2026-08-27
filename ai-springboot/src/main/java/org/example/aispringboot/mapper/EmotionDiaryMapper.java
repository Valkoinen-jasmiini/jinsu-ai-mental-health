package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.EmotionDiary;

@Mapper
public interface EmotionDiaryMapper extends BaseMapper<EmotionDiary> {
}
