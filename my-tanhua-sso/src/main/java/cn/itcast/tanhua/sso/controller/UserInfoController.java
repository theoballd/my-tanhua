package cn.itcast.tanhua.sso.controller;


import cn.itcast.tanhua.sso.Service.UserInfoService;
import cn.itcast.tanhua.sso.vo.ErrorResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("user")
public class UserInfoController {

    @Autowired
    UserInfoService userInfoService;


    /**
     * 完善个人信息-基本信息
     *
     * @param param
     * @return
     */
    @PostMapping("loginReginfo")
    public ResponseEntity<Object> saveUserInfo(@RequestBody Map<String, String> param,
                                               @RequestHeader("Authorization") String token){


        try {
            Boolean b = this.userInfoService.saveUserInfo(param, token);
            if(b){
                return ResponseEntity.ok(null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ErrorResult errorResult = ErrorResult.builder().errCode("000001").errMessage("保存用户信息失败！").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }

    @PostMapping("loginReginfo/head")
    public ResponseEntity<Object> saveUserLogo(@RequestParam("headPhoto") MultipartFile file,
                                               @RequestHeader("Authorization") String token) {

        try {
            Boolean bool = this.userInfoService.saveUserLogo(file, token);
            if (bool) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ErrorResult errorResult = ErrorResult.builder().errCode("000001").errMessage("保存用户头像失败！").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }
}
