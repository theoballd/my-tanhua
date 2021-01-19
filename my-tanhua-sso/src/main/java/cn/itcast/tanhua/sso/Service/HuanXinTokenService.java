package cn.itcast.tanhua.sso.Service;


import cn.itcast.tanhua.sso.config.HuanXinConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class HuanXinTokenService {


    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private HuanXinConfig huanXinConfig;

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public static final String REDIS_KEY = "HX_TOKEN";


    private String refreshToken(){
        String targetUrl=this.huanXinConfig.getUrl()+this.huanXinConfig.getOrgName()+"/"+this.huanXinConfig.getAppName()+"/token";

        Map<String, String> param = new HashMap<>();

        param.put("grant_type", "client_credentials");
        param.put("client_id", this.huanXinConfig.getClientId());
        param.put("client_secret", this.huanXinConfig.getClientSecret());

        ResponseEntity<String> postForEntity = this.restTemplate.postForEntity(targetUrl, param, String.class);
        if(postForEntity.getStatusCodeValue()!=200){
                    return null;
        }

        String body = postForEntity.getBody();

        try {
            JsonNode jsonNode = MAPPER.readTree(body);
            String access_token = jsonNode.get("access_token").asText();
            if(StringUtils.isNotBlank(access_token)){
                //将token保存5天,有效期为5天，环信接口返回的有效期为6天
                this.redisTemplate.opsForValue().set(REDIS_KEY, access_token, Duration.ofDays(5));
                return access_token;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getToken() {
        String token = this.redisTemplate.opsForValue().get(REDIS_KEY);
        if (StringUtils.isBlank(token)) {
            return this.refreshToken();
        }
        return token;
    }
}
