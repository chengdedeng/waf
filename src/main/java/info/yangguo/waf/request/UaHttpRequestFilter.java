package info.yangguo.waf.request;

import info.yangguo.waf.Constant;
import info.yangguo.waf.util.ConfUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
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
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String ua = httpRequest.headers().get("User-Agent");
            if (ua != null) {
                for (Pattern pat : ConfUtil.getPattern(FilterType.UA.name())) {
                    Matcher matcher = pat.matcher(ua);
                    if (matcher.find()) {
                        hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), FilterType.UA.name(), pat.toString());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
