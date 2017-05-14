package com.chinaredstar.waf.request;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;

/**
 * @author:杨果
 * @date:2017/4/11 上午11:28
 *
 * Description:
 *
 * HTTP Request拦截器抽象类
 */
public abstract class HttpRequestFilter {
    /**
     * httpRequest拦截逻辑
     *
     * @param httpRequest http请求
     * @return true:正则匹配成功,false:正则匹配失败
     */
    public abstract boolean doFilter(HttpObject httpRequest, ChannelHandlerContext channelHandlerContext);

    /**
     * 是否是黑名单
     *
     * @return 黑名单返回true, 白名单返回false, 白名单的实现类要重写次方法
     */
    public boolean isBlacklist() {
        return true;
    }

    /**
     * 记录hack日志
     *
     * @param realIp 用户IP
     * @param logger 日志logger
     * @param type   匹配的类型
     * @param cause  被拦截的原因
     */
    public void hackLog(Logger logger, String realIp, String type, String cause) {
        logger.warn("type:{},realIp:{},cause:{}", type, realIp, cause);
    }
}
