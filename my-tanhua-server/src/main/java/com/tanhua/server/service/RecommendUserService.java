package com.tanhua.server.service;


import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.dubbo.server.api.RecommendUserApi;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.springframework.stereotype.Service;

/**
 * 负责与dubbo服务进行交互
 */
@Service
public class RecommendUserService {

   @Reference(version = "1.0.0")
    private RecommendUserApi recommendUserApi;



    public TodayBest queryTodayBest(Long userId) {
        RecommendUser recommendUser = this.recommendUserApi.queryWithMaxScore(userId);
        if(null == recommendUser){
            return null;
        }

        TodayBest todayBest = new TodayBest();
        todayBest.setId(recommendUser.getUserId());

        //缘分值
        double score = Math.floor(recommendUser.getScore());//取整,98.2 -> 98
        todayBest.setFateValue(Double.valueOf(score).longValue());

        return todayBest;
    }


    //RecommendUserService
    public PageInfo<RecommendUser> queryRecommendUserList(Long id, Integer page, Integer pagesize) {
        return this.recommendUserApi.queryPageInfo(id,page,pagesize);
    }
}
