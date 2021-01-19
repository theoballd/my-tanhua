package com.tanhua.server.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.server.mapper.UserInfoMapper;
import com.tanhua.server.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserInfoService {


    @Autowired
    private UserInfoMapper userInfoMapper;

    public UserInfo queryUserInfoByUserId(Long userId) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.userInfoMapper.selectOne(queryWrapper);
    }


    //UserInfoService
    /**
     * 查询用户信息列表
     *
     * @param queryWrapper
     * @return
     */
    public List<UserInfo> queryUserInfoList(QueryWrapper<UserInfo> queryWrapper) {

        return this.userInfoMapper.selectList(queryWrapper);
    }
}
