package com.tanhua.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.api.RecommendUserApi;
import com.tanhua.dubbo.server.api.UserLikeApi;
import com.tanhua.dubbo.server.api.UserLocationApi;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.pojo.UserLocation;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.dubbo.server.vo.UserLocationVo;
import com.tanhua.server.enums.SexEnum;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.NearUserVo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;


@Service
public class TodayBestService {
    @Autowired
    private UserService userService;

    @Autowired
    private RecommendUserService recommendUserService;


    @Value("${tanhua.sso.default.recommend.users}")
    private String defaultRecommendUsers;


    @Value("${tanhua.sso.default.user}")
    private Long defaultUser;


    @Autowired
    private IMService imService;


    @Autowired
    private UserInfoService userInfoService;


    @Reference(version = "1.0.0")
    private UserLocationApi userLocationApi;

    @Reference(version = "1.0.0")
    private UserLikeApi userLikeApi;

    public TodayBest queryTodayBest(String token) {
        //校验token是否有效，通过sso的接口校验
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            //token非法或过期
            return null;
        }

        //token合法未过期则查询推荐用户（今日佳人）
        TodayBest todayBest = this.recommendUserService.queryTodayBest(user.getId());
        if (null == todayBest) {
            //给出默认的推荐用户
            todayBest = new TodayBest();
            todayBest.setId(defaultUser);
            todayBest.setFateValue(80L); //固定值
        }

        //补全个人信息
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(user.getId());
        if (null == userInfo) {
            return null;
        }

        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));
        todayBest.setGender(userInfo.getSex().name().toLowerCase());
        todayBest.setAge(userInfo.getAge());

        return todayBest;

    }

    /**
     * 查询推荐用户列表
     *
     * @param queryParam
     * @param token
     * @return
     */

    public PageResult queryRecommendation(String token, RecommendUserQueryParam queryParam) {
        System.out.println("queryParam" + queryParam.toString());
        //校验token
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            //token非法或已经过期
            return null;
        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(queryParam.getPage());
        pageResult.setPagesize(queryParam.getPagesize());
        pageResult.setCounts(0); //前端不参与计算，仅需要返回字段

        PageInfo<RecommendUser> pageInfo = this.recommendUserService.queryRecommendUserList(user.getId(), queryParam.getPage(), queryParam.getPagesize());


        List<RecommendUser> records = pageInfo.getRecords();
        System.out.println("records数量----" + records.size());
        if (CollectionUtils.isEmpty(records)) {
            //没有查询到推荐的用户列表。默认推荐列表
            String[] ss = StringUtils.split(defaultRecommendUsers, ",");
            for (String s : ss) {
                RecommendUser recommendUser = new RecommendUser();
                recommendUser.setUserId(Long.valueOf(s));
                recommendUser.setToUserId(user.getId());
                recommendUser.setScore(RandomUtils.nextDouble(70, 99));
                records.add(recommendUser);
            }
        }

        //填充个人信息

        //收集推荐用户的id
        HashSet<Long> userIds = new HashSet<>();
        HashMap<Long, Double> map = new HashMap<>();
        for (RecommendUser record : records) {
            userIds.add(record.getUserId());
            map.put(record.getUserId(), record.getScore());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();

        //用户id参数
        queryWrapper.in("user_id", userIds);


        /**
         //条件查询

         if (StringUtils.isNotEmpty(queryParam.getGender())) {
         //需要性别参数查询
         queryWrapper.eq("sex", StringUtils.equals(queryParam.getGender(), "man") ? 1:2);
         }

         if (StringUtils.isNotEmpty(queryParam.getCity())) {
         //需要城市参数查询
         queryWrapper.like("city", queryParam.getCity());
         }

         if (queryParam.getAge() != null) {
         //设置年龄参数，条件：小于等于
         queryWrapper.le("age", queryParam.getAge());
         }
         **/


        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);


        if (CollectionUtils.isEmpty(userInfoList)) {
            //没有查询到用户的基本信息
            return pageResult;
        }


        ArrayList<TodayBest> todayBests = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            TodayBest todayBest = new TodayBest();

            //添加用户基本信息
            todayBest.setId(userInfo.getUserId());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));
            todayBest.setGender(userInfo.getSex().getValue() == 1 ? "man" : "woman");
            todayBest.setAge(userInfo.getAge());
            todayBest.setFateValue(Double.valueOf(map.get(userInfo.getUserId())).longValue());
            todayBests.add(todayBest);
        }

        //按缘分值进行倒序排序
        Collections.sort(todayBests, (Comparator<TodayBest>) (o1, o2) -> new Long(o2.getFateValue() - o1.getFateValue()).intValue());

        pageResult.setItems(todayBests);
        return pageResult;

    }

    public List<NearUserVo> queryNearUser(String gender, String distance) {

        User user = UserThreadLocal.get();
        //查询当前用户的位置信息
        UserLocationVo userLocationVo = this.userLocationApi.queryByUserId(user.getId());

        Double longitude = userLocationVo.getLongitude();
        Double latitude = userLocationVo.getLatitude();

        //根据当前用户的位置信息查询附件的好友
        List<UserLocationVo> userLocationList = this.userLocationApi.queryUserFromLocation(longitude, latitude, Integer.valueOf(distance));

        if (CollectionUtils.isEmpty(userLocationList)) {
            return Collections.emptyList();
        }

        ArrayList<Object> userIds = new ArrayList<>();

        for (UserLocationVo locationVo : userLocationList) {
            userIds.add(locationVo.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        if (StringUtils.equalsIgnoreCase(gender, "man")) {
            queryWrapper.in("sex", SexEnum.MAN);
        } else if (StringUtils.equalsIgnoreCase(gender, "woman")) {
            queryWrapper.in("sex", SexEnum.WOMAN);
        }
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<NearUserVo> nearUserVoList = new ArrayList<>();

        for (UserLocationVo locationVo : userLocationList) {

            if (locationVo.getUserId().longValue() == user.getId().longValue()) {
                // 排除自己
                continue;
            }

            for (UserInfo userInfo : userInfoList) {
                if (locationVo.getUserId().longValue() == userInfo.getUserId().longValue()) {
                    NearUserVo nearUserVo = new NearUserVo();

                    nearUserVo.setUserId(userInfo.getUserId());
                    nearUserVo.setAvatar(userInfo.getLogo());
                    nearUserVo.setNickname(userInfo.getNickName());

                    nearUserVoList.add(nearUserVo);
                    break;
                }
            }
        }
        return nearUserVoList;
    }

    public List<TodayBest> queryCardsList() {
        User user = UserThreadLocal.get();
        int count = 50;

        PageInfo<RecommendUser> pageInfo = this.recommendUserService.queryRecommendUserList(user.getId(), 1, count);

        if (CollectionUtils.isEmpty(pageInfo.getRecords())) {
            //默认推荐列表

            String[] ss = StringUtils.split(defaultRecommendUsers, ',');
            for (String s : ss) {
                RecommendUser recommendUser = new RecommendUser();
                recommendUser.setUserId(Long.valueOf(s));
                recommendUser.setToUserId(user.getId());
                pageInfo.getRecords().add(recommendUser);
            }
        }


        List<RecommendUser> records = pageInfo.getRecords();
        int showCount = Math.min(10, records.size());

        ArrayList<RecommendUser> newRecords = new ArrayList<>();

        for (int i = 0; i < showCount; i++) {
            //随机选出推荐的好友

            newRecords.add(records.get(RandomUtils.nextInt(0, records.size() - 1)));
        }

        ArrayList<Long> userIds = new ArrayList<>();
        for (RecommendUser record : newRecords) {
            userIds.add(record.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);


        List<UserInfo> userInfos = this.userInfoService.queryUserInfoList(queryWrapper);
        List<TodayBest> todayBests = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            TodayBest todayBest = new TodayBest();
            todayBest.setId(userInfo.getUserId());
            todayBest.setAge(userInfo.getAge());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setGender(userInfo.getSex().name().toLowerCase());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));
            todayBest.setFateValue(0L);

            todayBests.add(todayBest);
        }

        return todayBests;
    }

    public boolean likeUser(Long likeUserId) {
        User user = UserThreadLocal.get();
        String id = this.userLikeApi.saveUserLike(user.getId(), likeUserId);
        if (StringUtils.isEmpty(id)) {
            return false;
        }

        if (this.userLikeApi.isMutualLike(user.getId(), likeUserId)) {
            //相互喜欢成为好友
            this.imService.contactUser(likeUserId);
        }
        return true;
    }

    public boolean disLikeUser(Long likeUserId) {
        User user = UserThreadLocal.get();
        return this.userLikeApi.deleteUserLike(user.getId(), likeUserId);
    }
}
