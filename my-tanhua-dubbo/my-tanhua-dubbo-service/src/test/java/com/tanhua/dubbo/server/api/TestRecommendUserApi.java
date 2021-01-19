package com.tanhua.dubbo.server.api;


import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.pojo.TimeLine;
import com.tanhua.dubbo.server.pojo.Users;
import com.tanhua.dubbo.server.pojo.Visitors;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.dubbo.server.vo.UserLocationVo;
import org.apache.commons.lang3.RandomUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestRecommendUserApi {
    @Reference(version = "1.0.0")
    private RecommendUserApi recommendUserApi;

 @Autowired
    private QuanZiApi quanZiApi;

    @Autowired
    private VisitorsApi visitorsApi;

    @Autowired
    private MongoTemplate mongoTemplate;


    @Autowired
    private UserLocationApi userLocationApi;

    @Test
    public void testQuery() {
        Double longitude = 121.512253;
        Double latitude = 31.24094;
        Integer range = 1000;
        List<UserLocationVo> userLocationVos = this.userLocationApi.queryUserFromLocation(longitude, latitude, range);
        for (UserLocationVo userLocationVo : userLocationVos) {
            System.out.println(userLocationVo);
        }
    }

    @Test
    public void testSaveTestData() {
        this.userLocationApi.updateUserLocation(1L, 121.512253,31.24094, "金茂大厦");
        this.userLocationApi.updateUserLocation(2L, 121.506377, 31.245105, "东方明珠广播电视塔");
        this.userLocationApi.updateUserLocation(10L, 121.508815,31.243844, "陆家嘴地铁站");
        this.userLocationApi.updateUserLocation(12L, 121.511999,31.239185, "上海中心大厦");
        this.userLocationApi.updateUserLocation(25L, 121.493444,31.240513, "上海市公安局");
        this.userLocationApi.updateUserLocation(27L, 121.494108,31.247011, "上海外滩美术馆");
        this.userLocationApi.updateUserLocation(30L, 121.462452,31.253463, "上海火车站");
        this.userLocationApi.updateUserLocation(32L, 121.81509,31.157478, "上海浦东国际机场");
        this.userLocationApi.updateUserLocation(34L, 121.327908,31.20033, "虹桥火车站");
        this.userLocationApi.updateUserLocation(38L, 121.490155,31.277476, "鲁迅公园");
        this.userLocationApi.updateUserLocation(40L, 121.425511,31.227831, "中山公园");
        this.userLocationApi.updateUserLocation(43L, 121.594194,31.207786, "张江高科");
    }


    @Test
    public void testQueryWithMaxScore(){
        System.out.println(this.recommendUserApi.queryWithMaxScore(1L));
        System.out.println(this.recommendUserApi.queryWithMaxScore(8L));
        System.out.println(this.recommendUserApi.queryWithMaxScore(26L));
    }

    @Test
    public void testQueryPageInfo(){
        System.out.println(this.recommendUserApi.queryPageInfo(1L,1,5));
        System.out.println(this.recommendUserApi.queryPageInfo(1L,2,5));
        System.out.println(this.recommendUserApi.queryPageInfo(1L,3,5));
    }

    @Test
    public void saveUsers(){
        this.mongoTemplate.save(new Users(ObjectId.get(),1L, 2L, System.currentTimeMillis()));
        this.mongoTemplate.save(new Users(ObjectId.get(),1L, 3L, System.currentTimeMillis()));
        this.mongoTemplate.save(new Users(ObjectId.get(),1L, 4L, System.currentTimeMillis()));
        this.mongoTemplate.save(new Users(ObjectId.get(),1L, 5L, System.currentTimeMillis()));
        this.mongoTemplate.save(new Users(ObjectId.get(),1L, 6L, System.currentTimeMillis()));
    }

    @Test
    public void testQueryList() {
        Criteria criteria = Criteria.where("userId").is(1L);
        List<Users> users = this.mongoTemplate.find(Query.query(criteria), Users.class);
        for (Users user : users) {
            System.out.println(user);
        }
    }

        @Test
        public void testSavePublish(){
            Publish publish = new Publish();
            publish.setUserId(1L);
            publish.setLocationName("上海市");
            publish.setSeeType(1);
            publish.setText("今天天气不错~");
            publish.setMedias(Arrays.asList("https://itcast-tanhua.oss-cn-shanghai.aliyuncs.com/images/quanzi/1.jpg"));
            boolean result = this.quanZiApi.savePublish(publish);
            System.out.println(result);
        }

    @Test
    public void testRecommendPublish(){
        //查询用户id为2的动态作为推荐动态的数据
        PageInfo<Publish> pageInfo = this.quanZiApi.queryPublishList(2L, 1, 10);
        for (Publish record : pageInfo.getRecords()) {

            TimeLine timeLine = new TimeLine();
            timeLine.setId(ObjectId.get());
            timeLine.setPublishId(record.getId());
            timeLine.setUserId(record.getUserId());
            timeLine.setDate(System.currentTimeMillis());

            this.mongoTemplate.save(timeLine, "quanzi_time_line_recommend");
        }
    }

    @Test
    public void testSave(){
        for (int i = 0; i < 100; i++) {
            Visitors visitors = new Visitors();

            visitors.setFrom("首页");
            visitors.setUserId(RandomUtils.nextLong(1,10));
            visitors.setVisitorUserId(RandomUtils.nextLong(11,50));

            this.visitorsApi.saveVisitor(visitors);
        }

        System.out.println("ok");

    }
}
