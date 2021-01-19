package com.tanhua.server;


import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.tanhua.server.service.RecommendUserService;
import com.tanhua.server.service.TodayBestService;
import com.tanhua.server.vo.TodayBest;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TestRecommendUserApi {
    @Autowired
    private TodayBestService todayBestService;

    @Autowired
    protected FastFileStorageClient storageClient;

    @Test
    public void testQueryTodayBest(){
        TodayBest todayBest = this.todayBestService.queryTodayBest("eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MTAwLCJleHAiOjE2MDk0MjUwMTN9.NTd7EtpFNqY9g8EKr643SevkZ9Ikr4C75-TV_8YvMNw");
        System.out.println(todayBest);
    }

    @Test
    public void testUpload(){
        String path = "F:\\1.jpg";
        File file = new File(path);

        try {
            StorePath storePath = this.storageClient.uploadFile(FileUtils.openInputStream(file), file.length(), "jpg", null);

            System.out.println(storePath); //StorePath [group=group1, path=M00/00/00/wKgfUV2GJSuAOUd_AAHnjh7KpOc1.1.jpg]
            System.out.println(storePath.getFullPath());//group1/M00/00/00/wKgfUV2GJSuAOUd_AAHnjh7KpOc1.1.jpg
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
