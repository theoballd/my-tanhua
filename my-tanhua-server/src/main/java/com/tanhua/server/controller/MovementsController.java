package com.tanhua.server.controller;


import com.tanhua.server.service.MovementsService;
import com.tanhua.server.service.QuanziMQService;
import com.tanhua.server.vo.Movements;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.VisitorsVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("movements")
public class MovementsController {


    @Autowired
    private MovementsService movementsService;

    @Autowired
    private QuanziMQService quanziMQService;

    /**
     * 发送动态
     *
     * @param textContent
     * @param location
     * @param multipartFile
     * @param token
     * @return
     */

    @PostMapping()
    public ResponseEntity<Void> savePublish(@RequestParam(value = "textContent", required = false) String textContent,
                                            @RequestParam(value = "location", required = false) String location,
                                            @RequestParam(value = "latitude", required = false) String latitude,
                                            @RequestParam(value = "longitude", required = false) String longitude,
                                            @RequestParam(value = "imageContent", required = false) MultipartFile[] multipartFile,
                                            @RequestHeader("Authorization") String token){

        try {
            String publishId = this.movementsService.savePublish(textContent, location, latitude, longitude, multipartFile, token);
            if (StringUtils.isNotEmpty(publishId)) {
                // 发送消息
                this.quanziMQService.publishMsg(publishId);
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询好友动态
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping
    public PageResult queryPublishList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                       @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        return this.movementsService.queryPublishList(page, pageSize,false);
    }


    /**
     * 查询推荐动态
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("recommend")
    public PageResult queryRecommendPublishList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        return this.movementsService.queryPublishList(page, pageSize, true);
    }

    /**
     * 点赞
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/like")
    public ResponseEntity<Long> likeComment(@PathVariable("id") String publishId) {
        try {
            Long likeCount = this.movementsService.likeComment(publishId);
            if (likeCount != null) {
                //发送点赞消息
                this.quanziMQService.likePublishMsg(publishId);
                return ResponseEntity.ok(likeCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 取消点赞
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/dislike")
    public ResponseEntity<Long> disLikeComment(@PathVariable("id") String publishId) {
        try {
            Long likeCount = this.movementsService.cancelLikeComment(publishId);
            if (null != likeCount) {
                //发送取消点赞消息
                this.quanziMQService.disLikePublishMsg(publishId);
                return ResponseEntity.ok(likeCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 喜欢
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/love")
    public ResponseEntity<Long> loveComment(@PathVariable("id") String publishId) {
        try {
            Long loveCount = this.movementsService.loveComment(publishId);
            if (null != loveCount) {
                //发送喜欢消息
                this.quanziMQService.lovePublishMsg(publishId);
                return ResponseEntity.ok(loveCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 取消喜欢
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/unlove")
    public ResponseEntity<Long> disLoveComment(@PathVariable("id") String publishId) {
        try {
            Long loveCount = this.movementsService.cancelLoveComment(publishId);
            if (null != loveCount) {
                //发送取消喜欢消息
                this.quanziMQService.disLovePublishMsg(publishId);
                return ResponseEntity.ok(loveCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询单条动态信息
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<Movements> queryById(@PathVariable("id") String publishId) {
        System.out.println("publishId="+publishId);
        try {
            Movements movements = this.movementsService.queryById(publishId);
            if(null != movements){
                //发送消息
                this.quanziMQService.queryPublishMsg(publishId);
                return ResponseEntity.ok(movements);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


    /**
     * 谁看过我
     *
     * @return
     */
    @GetMapping("visitors")
    public ResponseEntity<List<VisitorsVo>> queryVisitorsList(){
        try {
            List<VisitorsVo> list = this.movementsService.queryVisitorsList();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


}
