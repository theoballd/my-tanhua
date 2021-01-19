package com.tanhua.dubbo.server.api;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.UserLocation;
import com.tanhua.dubbo.server.vo.UserLocationVo;
import javafx.scene.effect.Light;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

@Service(version = "1.0.0")
public class UserLocationApiImpl implements UserLocationApi {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 更新用户地理位置
     *
     * @return
     */
    @Override
    public String updateUserLocation(Long userId, Double longitude, Double latitude, String address) {
        UserLocation userLocation = new UserLocation();
        userLocation.setAddress(address);
        userLocation.setLocation(new GeoJsonPoint(longitude,latitude));
        userLocation.setUserId(userId);
        Query query = Query.query(Criteria.where("userId").is(userLocation.getUserId()));
        UserLocation ul = this.mongoTemplate.findOne(query, UserLocation.class);
        if(null==ul){
            //新增
            userLocation.setId(ObjectId.get());
            userLocation.setCreated(System.currentTimeMillis());
            userLocation.setUpdated(userLocation.getCreated());
            userLocation.setLastUpdated(userLocation.getCreated());

            this.mongoTemplate.save(userLocation);

            return userLocation.getId().toHexString();
        }else {
            Update update = Update.update("location", userLocation.getLocation()).set("updated", System.currentTimeMillis()).set("address", userLocation.getAddress()).set("lastUpdated", ul.getUpdated());
            this.mongoTemplate.updateFirst(query, update, UserLocation.class);
        }
        return ul.getId().toHexString();
    }

    @Override
    public UserLocationVo queryByUserId(Long userId) {

        Query query = Query.query(Criteria.where("userId").is(userId));
        UserLocation userLocation = this.mongoTemplate.findOne(query, UserLocation.class);
        return UserLocationVo.format(userLocation);
    }

    @Override
    public List<UserLocationVo> queryUserFromLocation(Double longitude, Double latitude, Integer range) {
        //中心点
        GeoJsonPoint geoJsonPoint = new GeoJsonPoint(longitude, latitude);

        //转换为2dsphere的距离
          Distance distant = new Distance(range / 1000, Metrics.KILOMETERS);

          //画一个圆
        Circle circle = new Circle(geoJsonPoint, distant);
        Query query = Query.query(Criteria.where("location").withinSphere(circle));
        List<UserLocationVo> userLocationVos = UserLocationVo.formatToList(this.mongoTemplate.find(query, UserLocation.class));
        return userLocationVos;
    }
}
