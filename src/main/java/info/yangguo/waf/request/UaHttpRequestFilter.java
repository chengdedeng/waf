package info.yangguo.waf.request;

import info.yangguo.waf.Constant;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author:杨果
 * @date:2017/5/11 下午2:39
 * <p>
 * Description:
 * <p>
 * User-Agent黑名单拦截
 */
public class UaHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(UaHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, Map<String, Boolean> regexs) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            List<String> headerValues = Constant.getHeaderValues(originalRequest, "User-Agent");
            if (headerValues.size() > 0 && headerValues.get(0) != null) {
                for (Map.Entry<String, Boolean> regex : regexs.entrySet()) {
                    if (regex.getValue()) {
                        Pattern pattern = Pattern.compile(regex.getKey());
                        Matcher matcher = pattern.matcher(headerValues.get(0));
                        if (matcher.find()) {
                            hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "UserAgent", regex.getKey());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
