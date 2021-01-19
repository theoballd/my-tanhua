package cn.itcast.tanhua.sso.controller;


import cn.itcast.tanhua.sso.Service.UserService;
import cn.itcast.tanhua.sso.pojo.User;
import cn.itcast.tanhua.sso.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;


    @PostMapping("loginVerification")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> param) {
        try {
            String phone = param.get("phone");
            String code = param.get("verificationCode");

            Object data = userService.login(phone, code);

            if (data != null) {
            //登录成功
                return ResponseEntity.ok((HashMap<String,Object>)(data));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ErrorResult errorResult = ErrorResult.builder().errCode("000002").errMessage("登录失败！").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }



    @GetMapping("{token}")
    public User queryUserByToken(@PathVariable("token") String token) {
        log.info("token="+token);
        return this.userService.queryUserByToken(token);
    }
}
