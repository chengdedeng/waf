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
 * @date:2017/5/15 上午9:27
 * <p>
 * Description:
 */
public class WUrlHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(WUrlHttpRequestFilter.class);

    @Override
    public boolean isBlacklist() {
        return false;
    }

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, Set<RequestConfig.Rule> rules) {
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
            for (RequestConfig.Rule rule : rules) {
                if (rule.getIsStart()) {
                    Pattern pattern = Pattern.compile(rule.getRegex());
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "WUrl", rule.getRegex());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}