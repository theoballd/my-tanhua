package com.tanhua.server.controller;


import com.tanhua.server.service.TodayBestService;
import com.tanhua.server.utils.Cache;
import com.tanhua.server.vo.NearUserVo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("tanhua")
@Slf4j
public class TodayBestController {

    @Autowired
    private TodayBestService todayBestService;



    /**
     * 查询今日佳人
     *
     * @param token
     * @return
     */
    @GetMapping("todayBest")
    @Cache(time = "30")   //开启缓存
    public ResponseEntity<TodayBest> queryTodayBest(@RequestHeader("Authorization") String token) {

        try {
            TodayBest todayBest = this.todayBestService.queryTodayBest(token);
            if (null != todayBest) {
                return ResponseEntity.ok(todayBest);
            }
        } catch (Exception e) {
            log.error("查询今日佳人出错~ token = " + token, e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }


    /**
     * 查询推荐用户列表
     *
     * @param token
     * @param queryParam
     * @return
     */

    @Cache(time = "30")   //开启缓存
    @GetMapping("recommendation")
    public ResponseEntity<PageResult> queryRecommendation(@RequestHeader("Authorization") String token,
                                                                                                                 RecommendUserQueryParam queryParam) {

        try {
            PageResult pageResult = this.todayBestService.queryRecommendation(token, queryParam);
            if (null != pageResult) {
                return ResponseEntity.ok(pageResult);
            }
        } catch (Exception e) {
            log.error("查询推荐用户列表出错~ token = " + token, e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }


    /**
     * 搜附近
     *
     * @param gender
     * @param distance
     * @return
     */
    @GetMapping("search")
    public ResponseEntity<List<NearUserVo>> queryNearUser(@RequestParam(value = "gender", required = false) String gender,
                                                          @RequestParam(value = "distance", defaultValue = "2000") String distance) {
        try {
            List<NearUserVo> list = this.todayBestService.queryNearUser(gender, distance);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


    //TodayBestController

    /**
     * 探花
     *
     * @return
     */
    @GetMapping("cards")
    public ResponseEntity<List<TodayBest>> queryCardsList() {
        try {
            List<TodayBest> list = this.todayBestService.queryCardsList();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


    //TodayBestController
    /**
     * 喜欢
     *
     * @param likeUserId
     * @return
     */
    @GetMapping("{id}/love")
    public ResponseEntity<Void> likeUser(@PathVariable("id") Long likeUserId) {
        try {
            this.todayBestService.likeUser(likeUserId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 不喜欢
     *
     * @param likeUserId
     * @return
     */
    @GetMapping("{id}/unlove")
    public ResponseEntity<Void> disLikeUser(@PathVariable("id") Long likeUserId) {
        try {
            this.todayBestService.disLikeUser(likeUserId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
