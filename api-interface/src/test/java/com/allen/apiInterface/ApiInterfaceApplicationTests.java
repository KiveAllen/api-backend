package com.allen.apiInterface;

import com.api.apiclientsdk.client.ApiClient;
import com.api.apiclientsdk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApiInterfaceApplicationTests {

    // 注入一个名为yuApiClient的Bean
    @Resource
    private ApiClient apiClient;

//    // 表示这是一个测试方法
//    @Test
//    void contextLoads() {
//        // 调用yuApiClient的getNameByGet方法，并传入参数"yupi"，将返回的结果赋值给result变量
//        String result = apiClient.getNameByGet("allen");
//        // 创建一个User对象
//        User user = new User();
//        // 设置User对象的username属性为"KiveAllen"
//        user.setUserName("KiveAllen");
//        // 调用yuApiClient的getUserNameByPost方法，并传入user对象作为参数，将返回的结果赋值给usernameByPost变量
//        String usernameByPost = apiClient.getUserNameByPost(user);
//        // 打印result变量的值
//        System.out.println(result);
//        // 打印usernameByPost变量的值
//        System.out.println(usernameByPost);
//    }

}
