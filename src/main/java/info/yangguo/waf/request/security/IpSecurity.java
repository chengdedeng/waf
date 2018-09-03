package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.model.SecurityConfigItem;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
public class IpSecurity extends Security {
    private static final Logger logger = LoggerFactory.getLogger(IpSecurity.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, List<SecurityConfigItem> items) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());

            for (SecurityConfigItem item : items) {
                if (item.getConfig().getIsStart()) {
                    Timer itemTimer = Constant.metrics.timer("IpSecurity[" + item.getName() + "]");
                    Timer.Context itemContext = itemTimer.time();
                    try {
                        Pattern pattern = Pattern.compile(item.getName());
                        Matcher matcher = pattern.matcher(originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP));
                        if (matcher.find()) {
                            hackLog(logger, originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "Ip", item.getName());
                            return true;
                        }
                    } finally {
                        itemContext.stop();
                    }
                }
            }
        }
        return false;
    }
}
