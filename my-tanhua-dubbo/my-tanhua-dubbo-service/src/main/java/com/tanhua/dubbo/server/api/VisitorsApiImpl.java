package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.pojo.Visitors;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;


@Service(version = "1.0.0")
public class VisitorsApiImpl implements VisitorsApi {


    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public String saveVisitor(Visitors visitors) {
        visitors.setDate(System.currentTimeMillis());
        visitors.setId(ObjectId.get());

        this.mongoTemplate.save(visitors);

        return visitors.getId().toHexString();
    }

    @Override
    public List<Visitors> topVisitor(Long userId, Integer num) {

        Pageable pageable = PageRequest.of(0, num, Sort.by(Sort.Order.desc("date")));
        Query query = Query.query(Criteria.where("userId").is(userId)).with(pageable);
        return this.queryVisitorList(query);
    }

    @Override
    public List<Visitors> topVisitor(Long userId, Long date) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("date").gte(date));
        return this.queryVisitorList(query);
    }

    private List<Visitors> queryVisitorList(Query query) {
        List<Visitors> visitorsList = this.mongoTemplate.find(query, Visitors.class);

        // 查询得分
        for (Visitors visitors : visitorsList) {
            Query query1 = Query.query(Criteria.where("toUserId").is(visitors.getUserId()).and("userId").is(visitors.getVisitorUserId()));

            RecommendUser recommendUser = this.mongoTemplate.findOne(query1, RecommendUser.class);

            if(null!=recommendUser){
                visitors.setScore(recommendUser.getScore());
            }else {
                visitors.setScore(30d);
            }
        }
        return visitorsList;
    }
}
