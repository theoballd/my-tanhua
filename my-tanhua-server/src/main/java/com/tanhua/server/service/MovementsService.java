package com.tanhua.server.service;

import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.api.QuanZiApi;
import com.tanhua.dubbo.server.api.VideoApi;
import com.tanhua.dubbo.server.api.VisitorsApi;
import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.pojo.Visitors;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.utils.RelativeDateFormat;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.Movements;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.PicUploadResult;
import com.tanhua.server.vo.VisitorsVo;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import sun.awt.SunHints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class MovementsService {


    @Reference(version="1.0.0")
    private QuanZiApi quanZiApi;

    @Reference(version="1.0.0")
    private VisitorsApi visitorsApi;

    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoService userInfoService;


    @Autowired
    private PicUploadService picUploadService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    public String savePublish(String textContent,
                               String location,
                               String latitude,
                               String longitude,
                               MultipartFile[] multipartFile,
                               String token) {
        //查询当前登录信息
        User user = UserThreadLocal.get();

        Publish publish = new Publish();
        publish.setId(ObjectId.get());
        publish.setUserId(user.getId());
        publish.setText(textContent);
        publish.setLocationName(location);
        publish.setLatitude(latitude);
        publish.setLongitude(longitude);
        publish.setSeeType(1);

        //图片上传
        List<String> picUrls=new ArrayList<>();
        for (MultipartFile file : multipartFile) {
            PicUploadResult picUploadResult = this.picUploadService.upload(file);
            picUrls.add(picUploadResult.getName());
        }

        publish.setMedias(picUrls);
        boolean b = this.quanZiApi.savePublish(publish);
        if (!b){
            return null;
        }

        return String.valueOf(publish.getId());
    }

    /**
     * 查询好友动态
     *
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryPublishList(Integer page, Integer pageSize,boolean isRecommend) {
        //获取当前的登录信息
        User user = UserThreadLocal.get();

        PageInfo<Publish> pageInfo=null;
        if(isRecommend) {//推荐动态逻辑处理
            //查询Redis

            String value = this.redisTemplate.opsForValue().get("QUANZI_PUBLISH_RECOMMEND_" + user.getId());

            if (StringUtils.isNotEmpty(value)) {
                String[] pids = StringUtils.split(value, ",");
                int startIndex = (page - 1) * pageSize;
                if (startIndex < pids.length) {
                    int endIndex = startIndex + pageSize - 1;
                    if (endIndex >= pids.length) {
                        endIndex = pids.length - 1;
                    }

                    ArrayList<Long> pidsList = new ArrayList<>();

                    for (int i = startIndex; i <= endIndex; i++) {
                        pidsList.add(Long.valueOf(pids[i]));
                    }


                    List<Publish> publishList = this.quanZiApi.queryPublishByPids(pidsList);

                    pageInfo = new PageInfo<>();
                    pageInfo.setRecords(publishList);
                }
            }
        }
        if(pageInfo==null){
            Long userId=isRecommend?null:user.getId();
            pageInfo=this.quanZiApi.queryPublishList(userId,page,pageSize);
        }

        PageResult pageResult = new PageResult();


        pageResult.setPagesize(pageSize);
        pageResult.setPage(page);
        pageResult.setCounts(0);
       pageResult.setPages(0);

        List<Publish> records = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)){
            //没有动态信息
            return pageResult;
        }


        //将动态信息添加到返回对象的集合中
        List<Movements> movementsList = new ArrayList<Movements>();
        for (Publish record : records) {
            Movements movements = new Movements();
            movements.setId(record.getId().toHexString());
            movements.setImageContent(record.getMedias().toArray(new String[]{}));
            movements.setTextContent(record.getText());
            movements.setUserId(record.getUserId());
            movements.setCreateDate(RelativeDateFormat.format(new Date(record.getCreated())));
            movementsList.add(movements);
        }


         ArrayList<Long> userIds = new ArrayList<>();

        //j将动态的发布者的id添加到集合中
        for (Movements movements : movementsList) {
            if(!userIds.contains(movements.getUserId())){
                userIds.add(movements.getUserId());
            }
        }


        //将动态的个人信息通过id集合查询出来
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.in("user_id",userIds);
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(userInfoQueryWrapper);
        for (Movements movements : movementsList) {
            for (UserInfo userInfo : userInfoList) {
                //判断id集合中的id是否和动态信息中的发布者id是否想等，如果相等将发布者个人信息添加到返回对象中
                if(movements.getUserId().longValue()==userInfo.getUserId().longValue()){
                    this.fillValueToMovements(movements, userInfo);
                    break;
                }
            }
        }
        pageResult.setItems(movementsList);
        return pageResult;
    }

    /**
     * 点赞
     *
     * @param publishId
     * @return
     */
    public Long likeComment(String publishId) {
        User user = UserThreadLocal.get();
        boolean bool = this.quanZiApi.saveLikeComment(user.getId(), publishId);
        if(!bool){
            return null;
        }

        Long likeCount=0L;   //点赞数

        //保存是否点赞到redis,没有的话获取点赞数存入redis中，有的话点赞数加一
        String key="QUANZI_COMMENT_LIKE_"+publishId;
        if(!this.redisTemplate.hasKey(key)){
            Long count = this.quanZiApi.queryCommentCount(publishId, 1);
            likeCount=count;
            this.redisTemplate.opsForValue().set(key,String.valueOf(likeCount));
        }else {
            likeCount = this.redisTemplate.opsForValue().increment(key);
        }

        //记录已点赞存入redis中
        String userKey="QUANZI_COMMENT_LIKE_USER_"+user.getId()+"_"+publishId;
        this.redisTemplate.opsForValue().set(userKey,"1");

        return likeCount;

    }

    /**
     * 取消点赞
     *
     * @param publishId
     * @return
     */
    public Long cancelLikeComment(String publishId) {
        User user = UserThreadLocal.get();
        boolean bool = this.quanZiApi.removeComment(user.getId(), publishId, 1);
        if (bool) {
            String key = "QUANZI_COMMENT_LIKE_" + publishId;
            //数量递减
            Long likeCount = this.redisTemplate.opsForValue().decrement(key);

            //删除已点赞
            String userKey = "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_" + publishId;
            this.redisTemplate.delete(userKey);

            return likeCount;
        }
        return null;
    }

    /**
     * 喜欢
     *
     * @param publishId
     * @return
     */
    public Long loveComment(String publishId) {
        User user = UserThreadLocal.get();
        boolean bool=this.quanZiApi.saveLoveComment(user.getId(),publishId);
        if(!bool){
            return null;
        }
        Long loveCount=0L;

        //保存喜欢数到redis
        String key ="QUANZI_COMMENT_LOVE_" + publishId;
        if(!this.redisTemplate.hasKey(key)){
            Long count = this.quanZiApi.queryCommentCount(publishId, 3);
            loveCount=count;
            this.redisTemplate.opsForValue().set(key,String.valueOf(loveCount));
        }else {
          loveCount= this.redisTemplate.opsForValue().increment(key);
        }

        //记录已喜欢
        String userKey="QUANZI_COMMENT_LOVE_USER_"+user.getId()+"_"+publishId;
        this.redisTemplate.opsForValue().set(userKey,"1");

        return loveCount;

    }


    /**
     * 取消喜欢
     *
     * @return
     */
    public Long cancelLoveComment(String publishId) {
        User user = UserThreadLocal.get();
        boolean bool = this.quanZiApi.removeComment(user.getId(), publishId, 3);
        if (bool) {
            String key = "QUANZI_COMMENT_LOVE_" + publishId;
            //数量递减
            Long loveCount = this.redisTemplate.opsForValue().decrement(key);

            //删除已喜欢
            String userKey = "QUANZI_COMMENT_LOVE_USER_" + user.getId() + "_" + publishId;
            this.redisTemplate.delete(userKey);

            return loveCount;
        }
        return null;

    }


    /**
     * 查询单条动态信息
     *
     * @param publishId
     * @return
     */
    public Movements queryById(String publishId) {
        System.out.println("publishId="+publishId);
        Publish publish = this.quanZiApi.queryPublishById(publishId);
        if(publish==null){
            return null;
        }

        Movements movements = new Movements();
        movements.setId(publish.getId().toHexString());
        movements.setUserId(publish.getUserId());
        movements.setTextContent(publish.getText());
        movements.setImageContent(publish.getMedias().toArray(new String[]{}));
        movements.setCreateDate(RelativeDateFormat.format(new Date(publish.getCreated())));

        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(publish.getUserId());
        if (userInfo==null){
            return null;
        }

        this.fillValueToMovements(movements, userInfo);
        return movements;

    }

    private void fillValueToMovements(Movements movements, UserInfo userInfo) {
        User user = UserThreadLocal.get();
        movements.setAge(userInfo.getAge());
        movements.setAvatar(userInfo.getLogo());
        movements.setGender(userInfo.getSex().name().toLowerCase());
        movements.setNickname(userInfo.getNickName());
        movements.setTags(StringUtils.split(userInfo.getTags(),","));

        //获取评论数
        Long commentCount = this.quanZiApi.queryCommentCount(movements.getId(), 2);
        if(null == commentCount){
            movements.setCommentCount(0); //评论数
        }else{
            movements.setCommentCount(commentCount.intValue()); //评论数
        }

        movements.setDistance("1.2公里"); //TODO 距离

        String likeKey="QUANZI_COMMENT_LIKE_USER_"+user.getId()+"_"+movements.getId();

        movements.setHasLiked(this.redisTemplate.hasKey(likeKey) ? 1 : 0); //是否点赞（1是，0否）

        String likeKeyCount = "QUANZI_COMMENT_LIKE_" + movements.getId();
        String value = this.redisTemplate.opsForValue().get(likeKeyCount);
        if (StringUtils.isNotEmpty(value)) {
            movements.setLikeCount(Integer.valueOf(value)); //点赞数
        } else {
            movements.setLikeCount(0);
        }


        String LoveKey = "QUANZI_COMMENT_LOVE_USER_" + user.getId() + "_" + movements.getId();
        movements.setHasLoved(this.redisTemplate.hasKey(LoveKey) ? 1 : 0); //是否喜欢（1是，0否）

      String  loveKeyCount = "QUANZI_COMMENT_LOVE_" + movements.getId();
        value = this.redisTemplate.opsForValue().get(loveKeyCount);
        if (StringUtils.isNotEmpty(value)) {
            movements.setLoveCount(Integer.valueOf(value)); //喜欢数
        } else {
            movements.setLoveCount(0);
        }
    }

    public List<VisitorsVo> queryVisitorsList() {
        User user = UserThreadLocal.get();
        String redisKey = "visitor_date_" + user.getId();

        // 如果redis中存在上次查询的时间，就按照这个时间之后查询，如果没有就查询前5个
        List<Visitors> visitors = null;
        String value = this.redisTemplate.opsForValue().get(redisKey);
        if(StringUtils.isEmpty(value)){
            visitors = this.visitorsApi.topVisitor(user.getId(), 5);
        }else{
            visitors = this.visitorsApi.topVisitor(user.getId(), Long.valueOf(value));
        }

        if(CollectionUtils.isEmpty(visitors)){
            return Collections.emptyList();
        }

        List<Long> userIds = new ArrayList<>();
        for (Visitors visitor : visitors) {
            userIds.add(visitor.getVisitorUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<VisitorsVo> visitorsVoList = new ArrayList<>();

        for (Visitors visitor : visitors) {
            for (UserInfo userInfo : userInfoList) {
                if(visitor.getVisitorUserId().longValue() == userInfo.getUserId().longValue()){

                    VisitorsVo visitorsVo = new VisitorsVo();
                    visitorsVo.setAge(userInfo.getAge());
                    visitorsVo.setAvatar(userInfo.getLogo());
                    visitorsVo.setGender(userInfo.getSex().name().toLowerCase());
                    visitorsVo.setId(userInfo.getUserId());
                    visitorsVo.setNickname(userInfo.getNickName());
                    visitorsVo.setTags(StringUtils.split(userInfo.getTags(), ','));
                    visitorsVo.setFateValue(visitor.getScore().intValue());

                    visitorsVoList.add(visitorsVo);
                    break;
                }
            }
        }

        return visitorsVoList;
    }
}
