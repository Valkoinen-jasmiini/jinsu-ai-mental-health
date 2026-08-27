package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.ConsultationSession;

@Mapper
public interface ConsultationSessionMapper extends BaseMapper<ConsultationSession> {
}