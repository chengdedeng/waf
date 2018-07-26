package info.yangguo.waf.request;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.model.ItermConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author:杨果
 * @date:2017/5/11 下午3:17
 * <p>
 * Description:
 * <p>
 * Cookie黑名单拦截
 */
public class CookieHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CookieHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, List<ItermConfig> iterms) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            List<String> headerValues = Constant.getHeaderValues(originalRequest, "Cookie");
            if (headerValues.size() > 0 && headerValues.get(0) != null) {
                String[] cookies = headerValues.get(0).split(";");
                for (String cookie : cookies) {
                    for (ItermConfig iterm : iterms) {
                        if (iterm.getConfig().getIsStart()) {
                            Timer itermTimer = Constant.metrics.timer("CookieHttpRequestFilter[" + iterm.getName() + "]");
                            Timer.Context itermContext = itermTimer.time();
                            try {
                                Pattern pattern = Pattern.compile(iterm.getName());
                                Matcher matcher = pattern.matcher(cookie.toLowerCase());
                                if (matcher.find()) {
                                    hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "Cookie", iterm.getName());
                                    return true;
                                }
                            } finally {
                                itermContext.stop();
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
