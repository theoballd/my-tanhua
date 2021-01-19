package com.tanhua.server.service;


import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.dubbo.server.api.UserLocationApi;
import com.tanhua.dubbo.server.pojo.UserLocation;
import com.tanhua.server.pojo.User;
import com.tanhua.server.utils.UserThreadLocal;
import org.springframework.stereotype.Service;

@Service
public class BaiduService {

    @Reference(version = "1.0.0")
    private UserLocationApi userLocationApi;


    public Boolean updateLocation(Double longitude, Double latitude, String address) {
        try {
            User user = UserThreadLocal.get();

        this.userLocationApi.updateUserLocation(user.getId(), longitude, latitude, address);

        return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
