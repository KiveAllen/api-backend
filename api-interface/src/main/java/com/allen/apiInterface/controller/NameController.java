package com.allen.apiInterface.controller;

import com.api.apiclientsdk.model.User;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 名称 API
 *
 * @author allen
 */
@RestController
@RequestMapping("/")
public class NameController {
    @GetMapping("/name")
    public String getNameByGet(String name) {
        return "GET 你的名字是" + name;
    }

    @PostMapping("/")
    public String getNameByPost(@RequestParam String name) {
        return "POST 你的名字是" + name;
    }

    @PostMapping("/user")
    public String getUserNameByPost(@RequestBody User user, HttpServletRequest request) {
        // 从请求头中获取参数
        String accessKey = request.getHeader("accessKey");
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        String body = request.getHeader("body");

        // todo 实际情况应该是去数据库中查是否已分配给用户
//        if (!accessKey.equals("allen")) {
//            throw new RuntimeException("无权限");
//        }
        // 校验随机数，模拟一下，直接判断nonce是否大于10000
        if (Long.parseLong(nonce) > 10000) {
            throw new RuntimeException("无权限");
        }

        // 时间和当前时间不能超过5分钟
        long now = System.currentTimeMillis() / 1000;
        if (now - Long.parseLong(timestamp)> 5 * 60) {
            throw new RuntimeException("已经超过五分钟，签名过期");
        }

        // todo 实际情况中是从数据库查出 secretKey
//        String serverSign = SignUtils.genSign(body,"abcdefgh");
//        // 生成的签名不一致，抛异常
//        if(!sign.equals(serverSign)){
//            throw new RuntimeException("无权限");
//        }
        return "POST 用户名字是" + user.getUserName();
    }
}