package com.chinaredstar.waf.request;

import com.chinaredstar.waf.Constant;
import com.chinaredstar.waf.util.ConfUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/5/12 上午10:34
 *
 * Description:
 *
 * IP黑名单拦截
 */
public class IpHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(IpHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String realIp = Constant.getRealIp(httpRequest, channelHandlerContext);

            for (Pattern pat : ConfUtil.getPattern(FilterType.IP.name())) {
                Matcher matcher = pat.matcher(realIp);
                if (matcher.find()) {
                    hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), FilterType.IP.name(), pat.toString());
                    return true;
                }
            }
        }
        return false;
    }
}
