package com.tanhua.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.api.QuanZiApi;
import com.tanhua.dubbo.server.pojo.Comment;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.Comments;
import com.tanhua.server.vo.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class CommentsService {


    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;



    /**
     * 查询评论列表
     *
     * @param publishId
     * @param page
     * @param pagesize
     * @return
     */
    public PageResult queryCommentsList(String publishId, Integer page, Integer pagesize) {
        User user = UserThreadLocal.get();

        //查询出所有的评论
        PageInfo<Comment> pageInfo = this.quanZiApi.queryCommentList(publishId, page, pagesize);

        List<Comment> records = pageInfo.getRecords();

        if(records.isEmpty()){
            PageResult pageResult = new PageResult();
            pageResult.setPage(page);
            pageResult.setPagesize(pagesize);
            pageResult.setPages(0);
            pageResult.setCounts(0);
            return pageResult;
        }

        ArrayList<Long> userIds = new ArrayList<>();
        for (Comment comment : records) {
            if(!userIds.contains(comment.getUserId())){
                userIds.add(comment.getUserId());
            }
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id",userIds);
        //查询出所有评论人的个人信息
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        ArrayList<Comments> result = new ArrayList<>();
        for (Comment record : records) {
            Comments comments = new Comments();
            comments.setContent(record.getContent());
            comments.setCreateDate(new DateTime(record.getCreated()).toString("yyyy年MM月dd日 HH:mm"));
            comments.setId(record.getId().toHexString());

            for (UserInfo userInfo : userInfoList) {
                if(record.getUserId().longValue()==userInfo.getUserId().longValue()){
                    comments.setAvatar(userInfo.getLogo());
                    comments.setNickname(userInfo.getNickName());
                    break;
                }
            }

            String key = "QUANZI_COMMENT_LIKE_" + comments.getId();
            String value = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotEmpty(value)) {
                comments.setLikeCount(Integer.valueOf(value)); //点赞数
            } else {
                comments.setLikeCount(0);
            }

            String userKey = "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_" + comments.getId();
            comments.setHasLiked(this.redisTemplate.hasKey(userKey) ? 1 : 0); //是否点赞（1是，0否）

            result.add(comments);
        }

        PageResult pageResult=new PageResult();
        pageResult.setItems(result);
        pageResult.setPage(page);
        pageResult.setPagesize(pagesize);
        pageResult.setPages(0);
        pageResult.setCounts(0);

        return pageResult;
    }


    /**
     * 保存评论
     */

    public Boolean saveComments(String publishId, String content) {
        User user = UserThreadLocal.get();
        return this.quanZiApi.saveComment(user.getId(),publishId,2,content);
    }
}
