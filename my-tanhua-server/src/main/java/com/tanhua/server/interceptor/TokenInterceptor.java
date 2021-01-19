package com.tanhua.server.interceptor;

import com.tanhua.server.pojo.User;
import com.tanhua.server.service.UserService;
import com.tanhua.server.utils.NoAuthorization;
import com.tanhua.server.utils.UserThreadLocal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
         if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            NoAuthorization noAnnotation = handlerMethod.getMethod().getAnnotation(NoAuthorization.class);
            if (noAnnotation != null) {
                // 如果该方法被标记为无需验证token，直接返回即可
                return true;
            }
        }

        String token = request.getHeader("Authorization");
        if (StringUtils.isNotEmpty(token)) {
            User user = this.userService.queryUserByToken(token);
            if (null != user) {
                UserThreadLocal.set(user); //将当前对象，存储到当前的线程中
                return true;
            }
        }

        //请求头中如不存在Authorization直接返回false
        response.setStatus(401); //无权限访问
        return false;
    }
}
