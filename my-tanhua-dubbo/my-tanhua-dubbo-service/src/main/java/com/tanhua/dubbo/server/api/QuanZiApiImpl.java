package com.tanhua.dubbo.server.api;


import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.client.result.DeleteResult;
import com.tanhua.dubbo.server.pojo.*;
import com.tanhua.dubbo.server.service.IdService;
import com.tanhua.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

@Service(version = "1.0.0")
public class QuanZiApiImpl implements QuanZiApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IdService idService;


    @Override
    public boolean savePublish(Publish publish) {
        //校验
        if(publish.getUserId()==null){
            return false;
        }

        try {
            publish.setCreated(System.currentTimeMillis());//设置创建时间
            publish.setId(ObjectId.get());//设置id
            publish.setPid(this.idService.createId("publish", publish.getId().toHexString()));
            this.mongoTemplate.save(publish);  //保存发布

            Album album = new Album();  //创建相册对象，保存动态到自己的动态表中
            album.setCreated(System.currentTimeMillis());
            album.setId(ObjectId.get());
            album.setPublishId(publish.getId());
            this.mongoTemplate.save(album, "quanzi_album_" + publish.getUserId());

            //写入好友的时间线
            Criteria criteria = Criteria.where("userId").is(publish.getUserId());
            List<Users> users = this.mongoTemplate.find(Query.query(criteria), Users.class);

            for (Users user : users) {
                TimeLine timeLine = new TimeLine();
                timeLine.setId(ObjectId.get());
                timeLine.setPublishId(publish.getId());
                timeLine.setUserId(user.getUserId());
                timeLine.setDate(System.currentTimeMillis());
                this.mongoTemplate.save(timeLine, "quanzi_time_line_" + user.getFriendId());

                //TODO 写入自己的时间线

            }

            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public PageInfo<Publish> queryPublishList(Long userId, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = new Query().with(pageRequest);


        String collectionName="quanzi_time_line_"+userId;

        //如果未传入用户id，则表示查询的推荐用户列表
        if(userId==null) {
            collectionName = "quanzi_time_line_recommend";
        }


        //查询时间线表
        List<TimeLine> timeLines = this.mongoTemplate.find(query, TimeLine.class,"quanzi_time_line_"+userId);
        ArrayList<Object> publishIds = new ArrayList<>();
        for (TimeLine timeLine : timeLines) {
            //将时间线内的动态的id存入到集合中
            publishIds.add(timeLine.getPublishId());
        }

        //查询发布信息
        Query queryPublish = Query.query(Criteria.where("id").in(publishIds)).with(Sort.by(Sort.Order.desc("created")));
        //根据时间线表中的动态id查询动态表中的动态信息
        List<Publish> publishList = this.mongoTemplate.find(queryPublish, Publish.class);
        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(publishList);
        pageInfo.setTotal(0); //不提供总数
        return pageInfo;
    }



    /**
     * 点赞
     *
     * @param userId
     * @param publishId
     */
    @Override
    public boolean saveLikeComment(Long userId, String publishId) {
        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(publishId)).and("userId").is(userId).and("commentType").is(1));
        //根据用户的id和动态的id查询是否点赞
        long count = this.mongoTemplate.count(query, Comment.class);
   if(count>0){
       //如果点赞数大于0，说明点赞过了
       return false;
   }
   return this.saveComment(userId,publishId,1,null);

    }

    /**
     * 喜欢同上点赞
     *
     * @param userId
     * @param publishId
     */

    @Override
    public boolean saveLoveComment(Long userId, String publishId) {
        Query query = Query.query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(3));
        long count = this.mongoTemplate.count(query, Comment.class);
        if (count > 0) {
            return false;
        }
        return this.saveComment(userId, publishId, 3, null);
    }

    /**
     * 取消点赞、喜欢等
     *
     * @return
     */
    @Override
    public boolean removeComment(Long userId, String publishId, Integer commentType) {
        Query query = Query.query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(commentType));
        //删除此条点赞的数据
        DeleteResult remove = this.mongoTemplate.remove(query, Comment.class);
        return remove.getDeletedCount()>0;
    }


    /**
     * 保存评论
     *
     * @param userId
     * @param publishId
     * @param type
     * @return
     */
    @Override
    public boolean saveComment(Long userId, String publishId, Integer type, String content) {
        try {
            //将评论的信息存到评论表中
            Comment comment = new Comment();
            comment.setId(ObjectId.get());
            comment.setUserId(userId);
            comment.setContent(content);
            comment.setPublishId(new ObjectId(publishId));
            comment.setCreated(System.currentTimeMillis());
            comment.setCommentType(type);

            // 设置发布人的id
            Publish publish = this.mongoTemplate.findById(comment.getPublishId(), Publish.class);
            if (null != publish) {
                comment.setPublishUserId(publish.getUserId());
            } else {
                Video video = this.mongoTemplate.findById(comment.getPublishId(), Video.class);
                if (null != video) {
                    comment.setPublishUserId(video.getUserId());
                }
            }

            this.mongoTemplate.save(comment);
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 查询评论
     *
     */
    @Override
    public Long queryCommentCount(String publishId, Integer type) {
        //根据评论的id和评论的类型查询评论或者点赞数
        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(publishId)).and("commentType").is(type));
        return this.mongoTemplate.count(query,Comment.class);
    }


    //根据动态的id查询动态表
    @Override
    public Publish queryPublishById(String id) {
        return this.mongoTemplate.findById(new ObjectId(id),Publish.class);
    }

    @Override
    public PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.asc("created")));

        Query query = new Query(Criteria.where("publishId").is(new ObjectId(publishId))
                .and("commentType").is(2))
                .with(pageRequest);

        //查询动态表中该动态下的所有评论
        List<Comment> commentList = this.mongoTemplate.find(query, Comment.class);

        PageInfo<Comment> pageInfo = new PageInfo<>();

        pageInfo.setPageSize(pageSize);
        pageInfo.setPageNum(page);
        pageInfo.setRecords(commentList);
        pageInfo.setTotal(0);   //不提供总数
        return pageInfo;
    }



    /**
     * 查询用户的评论数据
     *
     * @return
     */
    @Override
    public PageInfo<Comment> queryCommentListByUser(Long userId, Integer type, Integer page, Integer pageSize) {
        PageRequest pageRequest= PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = new Query(Criteria.where("publishUserId").is(userId).and("commentType").is(type)).with(pageRequest);
        List<Comment> commentList = this.mongoTemplate.find(query, Comment.class);
        PageInfo<Comment> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(commentList);
        pageInfo.setTotal(0); //不提供总数
        return pageInfo;
    }

    @Override
    public List<Publish> queryPublishByPids(List<Long> pids){
        Query query = new Query(Criteria.where("pid").in(pids));
        return this.mongoTemplate.find(query, Publish.class);
    }
}
