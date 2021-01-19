package cn.itcast.tanhua.sso.controller;


import cn.itcast.tanhua.sso.Service.HuanXinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user/huanxin")
public class HuanXinController {
    @Autowired
    private HuanXinService huanXinService;

    /**
     * 添加联系人
     *
     * @param userId
     * @param friendId
     * @return
     */

    @PostMapping("contacts/{owner_username}/{friend_username}")
    public ResponseEntity<Void> contactUsers(@PathVariable("owner_username") Long userId,
                                             @PathVariable("friend_username") Long friendId) {
        try {
            boolean result = this.huanXinService.contactUsers(userId, friendId);
            if (result) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
