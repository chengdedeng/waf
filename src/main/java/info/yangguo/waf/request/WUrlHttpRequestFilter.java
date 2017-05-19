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
 * @date:2017/5/15 上午9:27
 *
 * Description:
 *
 */
public class WUrlHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(WUrlHttpRequestFilter.class);

    @Override
    public boolean isBlacklist() {
        return false;
    }

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String url;
            int index = httpRequest.getUri().indexOf("?");
            if (index > -1) {
                url = httpRequest.getUri().substring(0, index);
            } else {
                url = httpRequest.getUri();
            }
            for (Pattern pat : ConfUtil.getPattern(FilterType.WURL.name())) {
                Matcher matcher = pat.matcher(url);
                if (matcher.find()) {
                    hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), FilterType.WURL.name(), pat.toString());
                    return true;
                }
            }
        }
        return false;
    }
}