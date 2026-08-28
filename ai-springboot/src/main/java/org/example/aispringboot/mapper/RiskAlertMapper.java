package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.RiskAlert;

/**
 * 情绪风险预警 Mapper
 */
@Mapper
public interface RiskAlertMapper extends BaseMapper<RiskAlert> {
}
