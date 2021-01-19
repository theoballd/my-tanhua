package cn.itcast.tanhua.sso.Service;


import cn.itcast.tanhua.sso.config.HuanXinConfig;
import cn.itcast.tanhua.sso.vo.HuanXinUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;

@Service
public class HuanXinService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private HuanXinConfig huanXinConfig;


    @Autowired
    private HuanXinTokenService huanXinTokenService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 注册环信用户
     *
     * @param userId
     * @return
     */

    public boolean register(Long userId) {

        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users";

        String token = this.huanXinTokenService.getToken();

        try {
            //请求体
            HuanXinUser huanXinUser = new HuanXinUser(String.valueOf(userId), DigestUtils.md5Hex(userId + "_itcast_tanhua"));
            String body = MAPPER.writeValueAsString(Arrays.asList(huanXinUser));


            //请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer " + token);


            //请求体
            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(targetUrl, httpEntity, String.class);

            return responseEntity.getStatusCodeValue() == 200;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        // 注册失败
        return false;
    }

    /**
     * 添加好友
     *
     * @param userId
     * @param friendId
     * @return
     */
    public boolean contactUsers(Long userId, Long friendId) {
        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users/" +
                userId + "/contacts/users/" + friendId;


        try {
            String token = this.huanXinTokenService.getToken();
            //请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);

            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(targetUrl, httpEntity, String.class);

            return responseEntity.getStatusCodeValue() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 添加失败
        return false;
    }
}
