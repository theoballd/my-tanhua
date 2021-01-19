package cn.itcast.tanhua.sso.Service;

import cn.itcast.tanhua.sso.config.AliyunSMSConfig;
import cn.itcast.tanhua.sso.vo.ErrorResult;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
@Slf4j
public class SmsService {


    @Autowired
  private AliyunSMSConfig aliyunSMSConfig;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    public String sendSms(String mobile){
        DefaultProfile profile = DefaultProfile.getProfile(this.aliyunSMSConfig.getRegionId(),
                this.aliyunSMSConfig.getAccessKeyId(), this.aliyunSMSConfig.getAccessKeySecret());
        IAcsClient client = new DefaultAcsClient(profile);

        String code = RandomUtils.nextInt(100000, 999999) + "";

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(this.aliyunSMSConfig.getDomain());
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", this.aliyunSMSConfig.getRegionId());
        request.putQueryParameter("PhoneNumbers", mobile); //目标手机号
        request.putQueryParameter("SignName", this.aliyunSMSConfig.getSignName()); //签名名称
        request.putQueryParameter("TemplateCode", this.aliyunSMSConfig.getTemplateCode()); //短信模板code
        request.putQueryParameter("TemplateParam", "{\"code\":\""+code+"\"}");//模板中变量替换


        try {
            CommonResponse response = client.getCommonResponse(request);
            String data = response.getData();
            if(StringUtils.contains(data,"\"Message\":\"OK\"")){
                return code;
            }
            log.info("发送短信验证码失败~ data = " + data);
        } catch (Exception e) {
            log.error("发送短信验证码失败~ mobile = " + mobile, e);
        }
        return null;
    }


    /**
     * 发送短信验证码
     * 实现：发送完成短信验证码后，需要将验证码保存到redis中
     * @param phone
     * @return
     */
    public ErrorResult sendCheckCode(String phone) {

           String redisKey="CHECK_CODE_" + phone;

           //先判断该手机号的验证码是否还未失效
        if(this.redisTemplate.hasKey(redisKey)){
            String msg="上一次发送的验证码还未失效!";
            return  ErrorResult.builder().errCode("000001").errMessage(msg).build();
        }

//        String code = this.sendSms(phone);
        String code="123456";
        if(StringUtils.isEmpty(code)){
            String msg="发送短信验证码失败!";
            return ErrorResult.builder().errCode("000000").errMessage(msg).build();
        }

        //短信发送成功，将验证码存在redis中，有效期为5分钟

        this.redisTemplate.opsForValue().set(redisKey,code, Duration.ofMinutes(5));

        return null;
    }
}
