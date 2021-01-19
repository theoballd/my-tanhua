package com.tanhua.dubbo.server.api;


import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.client.result.DeleteResult;
import com.tanhua.dubbo.server.pojo.UserLike;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Service(version = "1.0.0")
public class UserLikeApiImpl implements UserLikeApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String saveUserLike(Long userId, Long likeUserId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("likeUserId").is(likeUserId));

        if(this.mongoTemplate.count(query, UserLike.class)>0){
            return null;
        }


        UserLike userLike = new UserLike();
        userLike.setId(ObjectId.get());
        userLike.setCreated(System.currentTimeMillis());
        userLike.setUserId(userId);
        userLike.setLikeUserId(likeUserId);

        this.mongoTemplate.save(userLike);
        return userLike.getId().toHexString();

    }

    @Override
    public Boolean isMutualLike(Long userId, Long likeUserId) {
        Criteria criteria = Criteria.where("userId").is(userId).and("likeUserId").is(likeUserId);
        Criteria criteria1 = Criteria.where("userId").is(likeUserId).and("likeUserId").is(userId);

        Criteria orOperator = new Criteria().orOperator(criteria, criteria1);

        return this.mongoTemplate.count(Query.query(orOperator),UserLike.class)==2;
    }

    @Override
    public Boolean deleteUserLike(Long userId, Long likeUserId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("likeUserId").is(likeUserId));
        DeleteResult deleteResult = this.mongoTemplate.remove(query, UserLike.class);
        return deleteResult.getDeletedCount()==1;
    }
}
