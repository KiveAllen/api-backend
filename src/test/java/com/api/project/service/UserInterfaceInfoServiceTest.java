package com.api.project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class UserInterfaceInfoServiceTest {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

//    @Test
//    public void invokeCount() {
//        //调用invokeCount，传参(1L,1L)
//        boolean f = userInterfaceInfoService.invokeCount(1L,1L);
//        //断言
//        Assertions.assertTrue(f);
//    }
}