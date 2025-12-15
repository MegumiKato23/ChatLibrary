package com.zg.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zg.ai.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
