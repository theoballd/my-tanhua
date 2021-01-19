package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.Users;
import com.tanhua.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@Service(version = "1.0.0")
public class UsersApiImpl implements UsersApi {

    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public String saveUsers(Users users) {
        if (users.getFriendId()==null||users.getUserId()==null) {
            return null;
        }
        Long userId = users.getUserId();
        Long friendId = users.getFriendId();

        Query query = Query.query(Criteria.where("userId").is(userId).and("friendId").is(friendId));
        Users oldUser = this.mongoTemplate.findOne(query, Users.class);
           if(null!=oldUser){
               return null;
           }

        //注册我与好友的关系
        users.setId(ObjectId.get());
        users.setDate(System.currentTimeMillis());
        this.mongoTemplate.save(users);


        //注册好友与我的关系
        users.setId(ObjectId.get());
        users.setUserId(friendId);
        users.setFriendId(userId);
        this.mongoTemplate.save(users);

        return users.getId().toHexString();
    }

    @Override
    public boolean removeUsers(Users users) {
        Long userId = users.getUserId();
        Long friendId = users.getFriendId();

        Query query1 = Query.query(Criteria.where("userId").is(userId)
                .and("friendId").is(friendId));

        //删除我与好友的关系数据
        long count1 = this.mongoTemplate.remove(query1, Users.class).getDeletedCount();

        Query query2 = Query.query(Criteria.where("userId").is(friendId)
                .and("friendId").is(userId));
        //删除好友与我的关系数据
        long count2 = this.mongoTemplate.remove(query2, Users.class).getDeletedCount();

        return count1 > 0 && count2 > 0;
    }


    @Override
    public List<Users> queryAllUsersList(Long userId) {
        Query query = Query.query(Criteria.where("userId").is(userId));
        return this.mongoTemplate.find(query, Users.class);
    }

    @Override
    public PageInfo<Users> queryUsersList(Long userId, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = Query.query(Criteria.where("userId").is(userId)).with(pageRequest);
        //查询出该用户所有好友关系
        List<Users> usersList = this.mongoTemplate.find(query, Users.class);
        PageInfo<Users> pageInfo = new PageInfo<>();
        pageInfo.setPageSize(pageSize);
        pageInfo.setPageNum(page);
        pageInfo.setRecords(usersList);
        pageInfo.setTotal(0); //不提供总数
        return pageInfo;
    }
}
