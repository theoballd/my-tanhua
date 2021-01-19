package com.tanhua.server.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.server.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class UserService {


    @Value("${tanhua.sso.url}")
    private String ssoUrl;

    @Autowired
    private RestTemplate restTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 通过sso的rest接口查询
     *
     * @param token
     * @return
     */
    public User queryUserByToken(String token){
        String url=ssoUrl+"/user/"+token;

        try {
            User user = this.restTemplate.getForObject(url, User.class);
            if (user==null) {
                return null;
            }
            return user;
        }catch (Exception e){
            log.error("校验token出错，token = " + token, e);
        }
        return null;
    }
}
