package com.api.apigateway;

import com.api.apiclientsdk.utils.SignUtils;
import com.api.apicommon.model.entity.InterfaceInfo;
import com.api.apicommon.model.entity.User;
import com.api.apicommon.service.InnerInterfaceInfoService;
import com.api.apicommon.service.InnerUserInterfaceInfoService;
import com.api.apicommon.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserService innerUserService;

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService;

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    //白名单，通常建议在权限管理中尽量使用白名单，少用黑名单
    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");

    private static final String INIERFACE_HOST = "http://localhost:8123";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 请求日志
        ServerHttpRequest request = exchange.getRequest();
        String path = INIERFACE_HOST + request.getPath().value();
        String method = request.getMethod().toString();
        log.info("请求唯一标识：" + request.getId());
        log.info("请求路径：" + path);
        log.info("请求方法：" + method);
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

        // 实际情况应该是去数据库中查是否已分配给用户
        User invokeUser = null;
        try {
            // 调用内部服务，根据访问秘钥获取用户信息
            invokeUser = innerUserService.getInvokeUser(accessKey);
        } catch (Exception e) {
            // 捕获异常，记录日志
            log.error("getInvokeUser error", e);
        }
        if (invokeUser == null) {
            // 如果用户信息为空，处理未授权情况
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
        // 从获取的用户信息中获取用户的秘钥
        String secretKey = invokeUser.getSecretKey();
        // 使用获取的秘钥签名
        String serverSign = SignUtils.genSign(body, secretKey);

        // 如果生成的签名不一致，则抛出异常，并提示"无权限"
        if (!sign.equals(serverSign)) {
            return handleNoAuth(response);
        }

        // 4. 从数据库中查询模拟接口是否存在，以及请求方法是否匹配(还可以校验请求参数)
        // 初始化一个 InterfaceInfo 对象，用于储存查询结果
        InterfaceInfo interfaceInfo = null;
        try {
            // 尝试从内部接口信息服务获取指定路径和方法的接口信息
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
        } catch (Exception e) {
            // 如果获取接口信息时出现异常，记录错误日志
            log.error("getInterfaceInfo error", e);
        }

        // 检查是否成功获取接口信息
        if (interfaceInfo == null) {
            // 如果为获取接口信息，放回处理未授权的响应
            return handleNoAuth(response);
        }

        // todo 是否还有调用次数

        return handleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());

    }

    /**
     * 处理响应
     *
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            // 获取原始的响应对象
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 获取数据缓冲工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 获取响应的状态码
            HttpStatus statusCode = originalResponse.getStatusCode();

            // 判断状态码是否为200 OK(按道理来说,现在没有调用,是拿不到响应码的,对这个保持怀疑 沉思.jpg)
            if (statusCode == HttpStatus.OK) {
                // 创建一个装饰后的响应对象(开始穿装备，增强能力)
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

                    // 重写writeWith方法，用于处理响应体的数据
                    // 这段方法就是只要当我们的模拟接口调用完成之后,等它返回结果，
                    // 就会调用writeWith方法,我们就能根据响应结果做一些自己的处理
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        // 判断响应体是否是Flux类型
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 返回一个处理后的响应体
                            // (这里就理解为它在拼接字符串,它把缓冲区的数据取出来，一点一点拼接好)
                            return super.writeWith(fluxBody.map(dataBuffer -> {
                                // 7. todo 调用成功，接口调用次数 + 1
                                try {
                                    // 调用内部用户接口信息服务，记录接口调用次数
                                    innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                } catch (Exception e) {
                                    // 捕获异常并记录错误信息
                                    log.error("invokeCount error", e);
                                }

                                // 读取响应体的内容并转换为字节数组
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);//释放掉内存
                                // 构建日志
                                List<Object> rspArgs = new ArrayList<>();
                                rspArgs.add(originalResponse.getStatusCode());
                                String data = new String(content, StandardCharsets.UTF_8);//data
                                // 6. 打印日志
                                log.info("响应结果：" + data);
                                // 将处理后的内容重新包装成DataBuffer并返回
                                return bufferFactory.wrap(content);
                            }));
                        } else {
                            // 8. 调用失败，返回一个规范的错误码
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 对于200 OK的请求,将装饰后的响应对象传递给下一个过滤器链,并继续处理(设置repsonse对象为装饰过的)
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            // 对于非200 OK的请求，直接返回，进行降级处理
            return chain.filter(exchange);
        } catch (Exception e) {
            // 处理异常情况，记录错误日志
            log.error("网关处理响应异常" + e);
            return chain.filter(exchange);
        }
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