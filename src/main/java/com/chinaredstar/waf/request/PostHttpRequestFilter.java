package com.chinaredstar.waf.request;

import com.chinaredstar.waf.Constant;
import com.chinaredstar.waf.util.ConfUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/5/15 下午2:33
 *
 * Description:
 *
 */
public class PostHttpRequestFilter extends HttpRequestFilter {
    private static Logger logger = LoggerFactory.getLogger(PostHttpRequestFilter.class);
    private static Pattern filePattern = Pattern.compile("Content-Disposition: form-data;(.+)filename=\"(.+)\\.(.*)\"");

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext ctx) {
        if (originalRequest.getMethod().name().equals("POST")) {
            if (httpObject instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) httpObject;
                HttpContent httpContent1 = httpContent.copy();
                String contentBody = new String(Unpooled.copiedBuffer(httpContent1.content()).array());
                List<Pattern> postPatternList = ConfUtil.getPattern(FilterType.POST.name());
                for (Pattern pattern : postPatternList) {
                    Matcher matcher = pattern.matcher(contentBody);
                    if (matcher.find()) {
                        hackLog(logger, Constant.getRealIp(originalRequest, ctx), FilterType.POST.name(), pattern.toString());
                        return true;
                    }
                }
                if (Constant.wafConfs.get("waf.file").equals("on")) {
                    Matcher fileMatcher = filePattern.matcher(contentBody);
                    if (fileMatcher.find()) {
                        String fileExt = fileMatcher.group(3);
                        for (Pattern pat : ConfUtil.getPattern(FilterType.FILE.name())) {
                            if (pat.matcher(fileExt).matches()) {
                                hackLog(logger, Constant.getRealIp(originalRequest, ctx), FilterType.POST.name(), filePattern.toString());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
