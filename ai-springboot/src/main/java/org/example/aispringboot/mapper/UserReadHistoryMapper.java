package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.aispringboot.entity.UserReadHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserReadHistoryMapper extends BaseMapper<UserReadHistory> {
}
