package info.yangguo.waf.request;

import info.yangguo.waf.Constant;
import info.yangguo.waf.model.RequestConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author:杨果
 * @date:2017/4/11 下午2:06
 * <p>
 * Description:
 * <p>
 * IP白名单拦截
 */
public class WIpHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(WIpHttpRequestFilter.class);

    @Override
    public boolean isBlacklist() {
        return false;
    }

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, Set<RequestConfig.Rule> rules) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            for (RequestConfig.Rule rule : rules) {
                if (rule.getIsStart()) {
                    Pattern pattern = Pattern.compile(rule.getRegex());
                    Matcher matcher = pattern.matcher(Constant.getRealIp(httpRequest, channelHandlerContext));
                    if (matcher.find()) {
                        hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "WIp", rule.getRegex());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
