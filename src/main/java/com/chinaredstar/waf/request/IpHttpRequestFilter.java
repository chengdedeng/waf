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
 * @date:2017/5/12 上午10:34
 *
 * Description:
 *
 */
public class IpHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(IpHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest httpRequest, ChannelHandlerContext channelHandlerContext) {
        String xRealIP = httpRequest.headers().get("X-Real-IP");
        String remoteAddress = channelHandlerContext.channel().remoteAddress().toString();
        String realIp;
        if (xRealIP != null) {
            realIp = xRealIP;
        } else {
            realIp = remoteAddress;
        }

        for (Pattern pat : ConfUtil.getPattern(FilterType.IP.name())) {
            Matcher matcher = pat.matcher(realIp);
            if (matcher.find()) {
                hackLog(logger, FilterType.IP.name(), pat.toString());
                return true;
            }
        }
        return false;
    }
}
