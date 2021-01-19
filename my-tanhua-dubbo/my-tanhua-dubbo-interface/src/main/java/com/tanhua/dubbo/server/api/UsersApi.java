package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.Users;
import com.tanhua.dubbo.server.vo.PageInfo;

import java.util.List;

public interface UsersApi {

    /**
     * 保存好友
     *
     * @param users
     * @return
     */
    String saveUsers(Users users);

    /**
     * 删除好友数据
     *
     * @param users
     * @return
     */
    boolean removeUsers(Users users);

    /**
     * 根据用户id查询Users列表
     *
     * @param userId
     * @return
     */
    List<Users> queryAllUsersList(Long userId);

    /**
     * 根据用户id查询Users列表(分页查询)
     *
     * @param userId
     * @return
     */
    PageInfo<Users> queryUsersList(Long userId, Integer page, Integer pageSize);
}
