package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.model.SecurityConfigIterm;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
public class UaSecurity extends Security {
    private static final Logger logger = LoggerFactory.getLogger(UaSecurity.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, List<SecurityConfigIterm> iterms) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String userAgent = originalRequest.headers().getAsString(HttpHeaderNames.USER_AGENT);
            if (userAgent != null) {
                for (SecurityConfigIterm iterm : iterms) {
                    if (iterm.getConfig().getIsStart()) {
                        Timer itermTimer = Constant.metrics.timer("UaSecurity[" + iterm.getName() + "]");
                        Timer.Context itermContext = itermTimer.time();
                        try {
                            Pattern pattern = Pattern.compile(iterm.getName());
                            Matcher matcher = pattern.matcher(userAgent);
                            if (matcher.find()) {
                                hackLog(logger, originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "UserAgent", iterm.getName());
                                return true;
                            }
                        } finally {
                            itermContext.stop();
                        }
                    }
                }
            }
        }
        return false;
    }
}
