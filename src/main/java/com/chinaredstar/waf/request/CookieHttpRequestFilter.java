package com.chinaredstar.waf.request;

import com.chinaredstar.waf.Constant;
import com.chinaredstar.waf.util.ConfUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/5/11 下午3:17
 *
 * Description:
 *
 * Cookie黑名单拦截
 */
public class CookieHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CookieHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest httpRequest, ChannelHandlerContext channelHandlerContext) {
        logger.debug("filter:{}", this.getClass().getName());
        String cookiesStr = httpRequest.headers().get(FilterType.COOKIE.name());
        if (cookiesStr != null) {
            String[] cookies = cookiesStr.split(";");
            for (String cookie : cookies) {
                for (Pattern pat : ConfUtil.getPattern(FilterType.COOKIE.name())) {
                    Matcher matcher = pat.matcher(cookie);
                    if (matcher.find()) {
                        hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), FilterType.COOKIE.name(), pat.toString());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
