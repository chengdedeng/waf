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
 * @date:2017/5/12 上午10:34
 * <p>
 * Description:
 * <p>
 * IP黑名单拦截
 */
public class IpHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(IpHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, Set<RequestConfig.Rule> rules) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String realIp = Constant.getRealIp(httpRequest, channelHandlerContext);

            for (RequestConfig.Rule rule : rules) {
                if (rule.getIsStart()) {
                    Pattern pattern = Pattern.compile(rule.getRegex());
                    Matcher matcher = pattern.matcher(realIp);
                    if (matcher.find()) {
                        hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "Ip", rule.getRegex());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
