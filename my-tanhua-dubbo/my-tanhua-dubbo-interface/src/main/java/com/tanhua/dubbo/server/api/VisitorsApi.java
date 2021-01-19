package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.Visitors;

import java.util.List;

public interface VisitorsApi {


    /**
     * 保存来访记录
     *
     * @param visitors
     * @return
     */
    String saveVisitor(Visitors visitors);

    /**
     * 按照时间倒序排序，查询最近的访客信息
     *
     * @param userId
     * @param num
     * @return
     */
    List<Visitors> topVisitor(Long userId, Integer num);

    /**
     * 按照时间倒序排序，查询最近的访客信息
     *
     * @param userId
     * @param date
     * @return
     */
    List<Visitors> topVisitor(Long userId, Long date);
}
