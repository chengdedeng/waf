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
 * @date:2017/5/11 下午2:24
 *
 * Description:
 *
 * URL路径黑名单拦截
 */
public class UrlHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(UrlHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest httpRequest, ChannelHandlerContext channelHandlerContext) {
        logger.debug("filter:{}", this.getClass().getName());
        String url;
        int index = httpRequest.getUri().indexOf("?");
        if (index > -1) {
            url = httpRequest.getUri().substring(0, index);
        } else {
            url = httpRequest.getUri();
        }
        for (Pattern pat : ConfUtil.getPattern(FilterType.URL.name())) {
            Matcher matcher = pat.matcher(FilterType.URL.name());
            if (matcher.find()) {
                hackLog(logger, FilterType.URL.name(), pat.toString());
                return true;
            }
        }
        return false;
    }
}
