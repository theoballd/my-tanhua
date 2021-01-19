package cn.itcast.tanhua.sso.Service;


import cn.itcast.tanhua.sso.mapper.UserMapper;
import cn.itcast.tanhua.sso.pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserService {

    @Autowired
  private   RedisTemplate<String,String> redisTemplate;

    @Autowired
   private UserMapper userMapper;

    @Autowired
    private HuanXinService huanXinService;

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;


    public Object login(String phone, String code) {
        String redisKey = "CHECK_CODE_" + phone;
        boolean isNew=false;

        //检验验证码
        String redisData = this.redisTemplate.opsForValue().get(redisKey);
        if(!StringUtils.equals(code,redisData)){
            return null;
        }


        //校验完成后，验证码需要废弃
        this.redisTemplate.delete(redisKey);

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("mobile",phone);

        User user = this.userMapper.selectOne(userQueryWrapper);

        if(null==user){
            //该用户为新用户，需要注册
             user= new User();

             user.setMobile(phone);
             user.setPassword(DigestUtils.md5Hex("123456"));

             this.userMapper.insert(user);
             isNew=true;
             this.huanXinService.register(user.getId());
        }


        //生成token

        HashMap<String, Object> claims = new HashMap<>();
        claims.put("id",user.getId());

        // 生成token
        String token = Jwts.builder()
                .setClaims(claims) //payload，存放数据的位置，不能放置敏感数据，如：密码等
                .signWith(SignatureAlgorithm.HS256, secret) //设置加密方法和加密盐
                .setExpiration(new DateTime().plusHours(12).toDate()) //设置过期时间，12小时后过期
                .compact();


        //RocketMQ发送登录成功的消息
        try {
            Map<String,Object> msg=new HashMap<>();

            msg.put("id",user.getId());
            msg.put("data",System.currentTimeMillis());

          this.rocketMQTemplate.convertAndSend("tanhua-ss0-login",msg);
        }catch (MessagingException e){
            log.error("发送消息失败！", e);
        }

              Map<String,Object> result=  new HashMap<>();
        result.put("token",token);
        result.put("isNew",isNew);
        return result;
    }

    public User queryUserByToken(String token) {
        try {
            // 通过token解析数据
            Map<String, Object> body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();

            User user = new User();
            user.setId(Long.valueOf(body.get("id").toString()));
            user.setMobile(this.userMapper.selectById(user.getId()).getMobile());
            return user;
        } catch (ExpiredJwtException e) {
            log.info("token已经过期！ token = " + token);
        } catch (Exception e) {
            log.error("token不合法！ token = "+ token, e);
        }
        return null;
    }
}
