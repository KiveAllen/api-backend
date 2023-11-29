package com.api.apigateway;

import com.api.apiclientsdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    //白名单，通常建议在权限管理中尽量使用白名单，少用黑名单
    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 请求日志
        ServerHttpRequest request = exchange.getRequest();
        log.info("请求唯一标识：" + request.getId());
        log.info("请求路径：" + request.getPath().value());
        log.info("请求方法：" + request.getMethod());
        log.info("请求参数：" + request.getQueryParams());

        String sourceAddress = Objects.requireNonNull(request.getRemoteAddress()).getHostString();
        log.info("请求来源地址：" + sourceAddress);
        log.info("请求来源地址：" + request.getRemoteAddress());

        // 2. 访问控制 - 黑白名单
        // 拿到相应对象
        ServerHttpResponse response = exchange.getResponse();
        if (!IP_WHITE_LIST.contains(sourceAddress)) {
            // 设置响应状态为403 Forbidden（禁止访问）
            response.setStatusCode(HttpStatus.FORBIDDEN);
            // 返回处理完成的响应
            return response.setComplete();
        }

        // 3. 用户鉴权 (判断 ak、sk 是否合法)
        // 从请求头中获取参数
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("nonce");
        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");
        String body = headers.getFirst("body");

        // todo 实际情况应该是去数据库中查是否已分配给用户
        if (!accessKey.equals("5192875814318ddcd5e3b0051560b42b")) {
            return handleNoAuth(response);
        }
        // 直接校验如果随机数大于1万，则抛出异常，并提示"无权限"
        if (Long.parseLong(nonce) > 10000) {
            return handleNoAuth(response);
        }

        // 时间和当前时间不能超过5分钟
        Long currentTime = System.currentTimeMillis() / 1000;
        final Long FIVE_MINUTES = 60 * 5L;
        if ((currentTime - Long.parseLong(timestamp)) >= FIVE_MINUTES) {
            return handleNoAuth(response);
        }

        // 实际情况中是从数据库中查出 secretKey
        String serverSign = SignUtils.genSign(body, "a5bc1401d4b52fc67a64282ace66428c");
        // 如果生成的签名不一致，则抛出异常，并提示"无权限"
        if (!sign.equals(serverSign)) {
            return handleNoAuth(response);
        }

        // 4. 请求的模拟接口是否存在?
        // todo 从数据库中查询模拟接口是否存在，以及请求方法是否匹配(还可以校验请求参数)

        // 5. 请求转发，调用模拟接口
        Mono<Void> filter = chain.filter(exchange);

        // 6. 响应日志
        log.info("响应：" + response.getStatusCode());

        // 7. todo 调用成功，接口调用次数 + 1
        if(response.getStatusCode()==HttpStatus.OK){

        }else {
            // 8. 调用失败，返回一个规范的错误码
            return handleInvokeError(response);
        }
        return filter;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    public Mono<Void> handleNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    public Mono<Void> handleInvokeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }
}