package cn.itcast.tanhua.sso;


import cn.itcast.tanhua.sso.Service.FaceEngineService;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSendSms {

    @Autowired
    private FaceEngineService faceEngineService;

    @Test
    public void testCheckIsPortrait(){
        File file = new File("F:\\2.jpg");
        boolean checkIsPortrait = this.faceEngineService.checkIsPortrait(file);
        System.out.println(checkIsPortrait); // true|false
    }



    @Test
    public void testSend(){
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou",
                "LTAI4GKYG6FJPQQJuSap39sQ", "rxNstNFNlTPeog2n18FkvazlwSGsg9");
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", "cn-hangzhou");
        request.putQueryParameter("PhoneNumbers", "19918026376"); //目标手机号
        request.putQueryParameter("SignName", "青橙"); //签名名称
        request.putQueryParameter("TemplateCode", "SMS_205136201"); //短信模板code
        request.putQueryParameter("TemplateParam", "{\"code\":\"123456\"}");//模板中变量替换
        try {
            CommonResponse response = client.getCommonResponse(request);

            //{"Message":"OK","RequestId":"EC2D4C9A-0EAC-4213-BE45-CE6176E1DF23","BizId":"110903802851113360^0","Code":"OK"}
            System.out.println(response.getData());
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

}
