package cn.itcast.tanhua.sso.controller;

import cn.itcast.tanhua.sso.Service.SmsService;
import cn.itcast.tanhua.sso.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/user")
@Slf4j
public class SmsController {


    @Autowired
    private SmsService smsService;

    @PostMapping("/login")
    public ResponseEntity<ErrorResult> snedCheckCode(@RequestBody Map<String,String> param){
        String phone = param.get("phone");
        ErrorResult errorResult = null;

        try {
            errorResult=this.smsService.sendCheckCode(phone);
            if (errorResult==null){
                return ResponseEntity.ok(null);
            }
        }catch (Exception e){
            log.error("发送短信验证码失败~ phone = " + phone, e);
          errorResult= ErrorResult.builder().errCode("000002").errMessage("短信验证码发送失败！").build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }
}
