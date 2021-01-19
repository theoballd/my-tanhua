package cn.itcast.tanhua.sso.Service;

import cn.itcast.tanhua.sso.enums.SexEnum;
import cn.itcast.tanhua.sso.mapper.UserInfoMapper;
import cn.itcast.tanhua.sso.pojo.User;
import cn.itcast.tanhua.sso.pojo.UserInfo;
import cn.itcast.tanhua.sso.vo.PicUploadResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public class UserInfoService {


    @Autowired
  private   UserService userService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private FaceEngineService faceEngineService;


    @Autowired
    private PicUploadService picUploadService;

    public Boolean saveUserInfo(Map<String, String> param, String token) {

        //校验token
        User user = this.userService.queryUserByToken(token);

        if(user==null){
            return false;
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setSex(StringUtils.equalsIgnoreCase(param.get("garden"),"man")? SexEnum.MAN:SexEnum.WOMAN);
        userInfo.setNickName(param.get("nickname"));
        userInfo.setBirthday(param.get("birthday"));
        userInfo.setCity(param.get("city"));

        return this.userInfoMapper.insert(userInfo)==1;
    }


    public Boolean saveUserLogo(MultipartFile file, String token) {
      //检验token
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }


        //校验图片是否是人像，如果不是人像就返回false
        try {
            boolean b = this.faceEngineService.checkIsPortrait(file.getBytes());
            if (!b){
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        //图片上传到阿里云OSS
        PicUploadResult upload = this.picUploadService.upload(file);
       if(StringUtils.isEmpty( upload.getName())){
           //如果文件地址为空，上传失败
           return false;
       }


        //把头像保存到用户信息表中
        UserInfo userInfo = new UserInfo();
       userInfo.setLogo(upload.getName());
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("user_id",user.getId());
      return this.userInfoMapper.update(userInfo, userInfoQueryWrapper)==1;
    }
}
