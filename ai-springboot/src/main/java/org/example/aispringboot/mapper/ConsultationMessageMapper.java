package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.ConsultationMessage;

@Mapper
public interface ConsultationMessageMapper extends BaseMapper<ConsultationMessage> {
}
