package com.tanhua.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tanhua.dubbo.server.api.QuanZiApi;
import com.tanhua.dubbo.server.api.UsersApi;
import com.tanhua.dubbo.server.pojo.Comment;
import com.tanhua.dubbo.server.pojo.Users;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.pojo.Announcement;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.Contacts;
import com.tanhua.server.vo.MessageAnnouncement;
import com.tanhua.server.vo.MessageLike;
import com.tanhua.server.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class IMService {


    @Reference(version = "1.0.0")
    private UsersApi usersApi;

    @Autowired
    private RestTemplate restTemplate;

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Value("${tanhua.sso.url}")
    private String url;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private AnnouncementService announcementService;


    /**
     * 添加好友
     *
     * @param userId
     */
    public boolean contactUser(Long userId) {
        User user = UserThreadLocal.get();
        Users users = new Users();
        users.setUserId(user.getId());
        users.setFriendId(userId);

        //保存好友关系到mongDB中
        String id = this.usersApi.saveUsers(users);

        if (StringUtils.isNotEmpty(id)) {

            try {
                //发送请求到sso系统   注册好友关系到环信
                String targetUrl = url + "/user/huanxin/contacts/" + users.getUserId() + "/" + users.getFriendId();
                ResponseEntity<Void> responseEntity = this.restTemplate.postForEntity(targetUrl, null, Void.class);
                if(responseEntity.getStatusCode().is2xxSuccessful()){
                    return true;
                }
            }catch (Exception e){
                //添加好友失败，删除Mongodb中的好友数据 事务
                this.usersApi.removeUsers(users);
                log.error("添加环信好友失败！userId = "+ user.getId()+", friendId = " + userId);
            }
            return false;
        }
        return false;
    }

    public PageResult queryContactsList(Integer page, Integer pageSize, String keyword) {
        User user = UserThreadLocal.get();
        List<Users> userList=null;
        if(StringUtils.isNotEmpty(keyword)){
            //关键字不为空，查询所有的好友，在后面进行关键字过滤
            List<Users> usersList = this.usersApi.queryAllUsersList(user.getId());
        }else {
            //关键字为空，进行分页
            PageInfo<Users> usersPageInfo = this.usersApi.queryUsersList(user.getId(), page, pageSize);
            userList=usersPageInfo.getRecords();
        }
        ArrayList<Long> userIds = new ArrayList<>();
        for (Users users : userList) {
            //获取所有朋友的id
         userIds.add(users.getFriendId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id",userIds);
        if(StringUtils.isNotEmpty(keyword)){
            queryWrapper.like("nick_name",keyword);
        }

        //获取好友的详细信息
        List<UserInfo> userInfos = this.userInfoService.queryUserInfoList(queryWrapper);


        List<Contacts> contactsList = new ArrayList<>();

        //填充用户信息
        for (UserInfo userInfo : userInfos) {
            Contacts contacts = new Contacts();
            contacts.setAge(userInfo.getAge());
            contacts.setAvatar(userInfo.getLogo());
            contacts.setGender(userInfo.getSex().name().toLowerCase());
            contacts.setNickname(userInfo.getNickName());
            contacts.setUserId(String.valueOf(userInfo.getUserId()));
            contacts.setCity(StringUtils.substringBefore(userInfo.getCity(), "-"));

            contactsList.add(contacts);
        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPagesize(pageSize);
        pageResult.setItems(contactsList);

        return pageResult;
    }

    public PageResult queryMessageLikeList(Integer page, Integer pageSize) {
        return this.messageCommentList(1, page, pageSize);
    }


    public PageResult queryMessageCommentList(Integer page, Integer pageSize) {
        return this.messageCommentList(2, page, pageSize);
    }

    public PageResult queryMessageLoveList(Integer page, Integer pageSize) {
        return this.messageCommentList(3, page, pageSize);
    }



    private PageResult messageCommentList(Integer type, Integer page, Integer pageSize) {
        User user = UserThreadLocal.get();

        //获取所有点赞信息

        PageInfo<Comment> pageInfo = this.quanZiApi.queryCommentListByUser(user.getId(), type, page, pageSize);

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPagesize(pageSize);


        List<Comment> records = pageInfo.getRecords();


        ArrayList<Long> userIds = new ArrayList<>();
        for (Comment comment : records) {
            userIds.add(comment.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id",userIds);

        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<MessageLike> messageLikeList = new ArrayList<>();
        for (Comment record : records) {
            for (UserInfo userInfo : userInfoList) {
                if (userInfo.getUserId().longValue() == record.getUserId().longValue()) {

                    MessageLike messageLike = new MessageLike();
                    messageLike.setId(record.getId().toHexString());
                    messageLike.setAvatar(userInfo.getLogo());
                    messageLike.setNickname(userInfo.getNickName());
                    messageLike.setCreateDate(new DateTime(record.getCreated()).toString("yyyy-MM-dd HH:mm"));

                    messageLikeList.add(messageLike);
                    break;
                }
            }
        }

        pageResult.setItems(messageLikeList);
        return pageResult;
    }

    public PageResult queryMessageAnnouncementList(Integer page, Integer pageSize) {

        IPage<Announcement> announcementPage = this.announcementService.queryList(page, pageSize);

        List<MessageAnnouncement> messageAnnouncementList = new ArrayList<>();

        for (Announcement record : announcementPage.getRecords()) {
            MessageAnnouncement messageAnnouncement = new MessageAnnouncement();
            messageAnnouncement.setId(record.getId().toString());
            messageAnnouncement.setTitle(record.getTitle());
            messageAnnouncement.setDescription(record.getDescription());
            messageAnnouncement.setCreateDate(new DateTime(record.getCreated()).toString("yyyy-MM-dd HH:mm"));

            messageAnnouncementList.add(messageAnnouncement);
        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPagesize(pageSize);
        pageResult.setItems(messageAnnouncementList);

        return pageResult;
    }
}
