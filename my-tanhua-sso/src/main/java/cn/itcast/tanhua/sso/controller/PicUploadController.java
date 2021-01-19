package cn.itcast.tanhua.sso.controller;


import cn.itcast.tanhua.sso.Service.PicUploadService;
import cn.itcast.tanhua.sso.vo.PicUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/pic")
@RestController
public class PicUploadController {

    @Autowired
    private PicUploadService picUploadService;

    @PostMapping("/upload")
    public PicUploadResult upload(@RequestParam("file") MultipartFile multipartFile) {
        return this.picUploadService.upload(multipartFile);
    }
}
