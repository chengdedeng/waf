package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.model.SecurityConfigItem;
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
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, List<SecurityConfigItem> items) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String userAgent = originalRequest.headers().getAsString(HttpHeaderNames.USER_AGENT);
            if (userAgent != null) {
                for (SecurityConfigItem item : items) {
                    if (item.getConfig().getIsStart()) {
                        Timer itemTimer = Constant.metrics.timer("UaSecurity[" + item.getName() + "]");
                        Timer.Context itemContext = itemTimer.time();
                        try {
                            Pattern pattern = Pattern.compile(item.getName());
                            Matcher matcher = pattern.matcher(userAgent);
                            if (matcher.find()) {
                                hackLog(logger, originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "UserAgent", item.getName());
                                return true;
                            }
                        } finally {
                            itemContext.stop();
                        }
                    }
                }
            }
        }
        return false;
    }
}
