package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.model.SecurityConfigIterm;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * @author:杨果
 * @date:2017/5/11 上午9:45
 * <p>
 * Description:
 * <p>
 * URL参数黑名单参数拦截
 */
public class ArgsSecurityFilter extends SecurityFilter {
    private static final Logger logger = LoggerFactory.getLogger(ArgsSecurityFilter.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, List<SecurityConfigIterm> iterms) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;

            int index = httpRequest.uri().indexOf("?");
            if (index > -1) {
                String argsStr = httpRequest.uri().substring(index + 1);
                List<NameValuePair> args = URLEncodedUtils.parse(argsStr, UTF_8);
                for (NameValuePair arg : args) {
                    for (SecurityConfigIterm iterm : iterms) {
                        if (iterm.getConfig().getIsStart()) {
                            Timer itermTimer = Constant.metrics.timer("ArgsHttpRequestFilter[" + iterm.getName() + "]");
                            Timer.Context itermContext = itermTimer.time();
                            try {
                                Pattern pattern = Pattern.compile(iterm.getName());
                                Matcher matcher = pattern.matcher(arg.getName().toLowerCase() + "=" + arg.getValue().toLowerCase());
                                if (matcher.find()) {
                                    hackLog(logger, httpRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "Args", iterm.getName());
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
