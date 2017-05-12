package com.chinaredstar.waf.request;

import com.chinaredstar.waf.util.ConfUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/5/11 下午2:39
 *
 * Description:
 *
 * User-Agent黑名单拦截
 */
public class UaHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(UaHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest httpRequest, ChannelHandlerContext channelHandlerContext) {
        logger.debug("filter:{}", this.getClass().getName());
        String ua = httpRequest.headers().get("User-Agent");
        if (ua != null) {
            for (Pattern pat : ConfUtil.getPattern(FilterType.UA.name())) {
                Matcher matcher = pat.matcher(ua);
                if (matcher.find()) {
                    hackLog(logger, FilterType.UA.name(), pat.toString());
                    return true;
                }
            }
        }
        return false;
    }
}
