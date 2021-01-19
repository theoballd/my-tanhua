package com.tanhua.server.controller;


import com.tanhua.dubbo.server.vo.HuanXinUser;
import com.tanhua.server.pojo.User;
import com.tanhua.server.utils.UserThreadLocal;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("huanxin")
public class HuanXinController {


    /**
     * 返回环信注册的用户名和密码
     *
     * @param
     * @return
     */

    @GetMapping("user")
    public ResponseEntity<HuanXinUser> queryHuanXinUser() {
        User user = UserThreadLocal.get();

        HuanXinUser huanXinUser = new HuanXinUser();
        huanXinUser.setUsername(user.getId().toString());
        huanXinUser.setPassword(DigestUtils.md5Hex(user.getId() + "_itcast_tanhua"));

        return ResponseEntity.ok(huanXinUser);
    }
}
