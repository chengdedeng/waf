package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.model.SecurityConfigIterm;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String url = null;
            try {
                String uri = httpRequest.uri().replaceAll("%", "%25");
                url = URLDecoder.decode(uri, "UTF-8");
            } catch (Exception e) {
                logger.warn("URL:{} is inconsistent with the iterms", httpRequest.uri(), e);
            }
            if (url != null) {
                int index = url.indexOf("?");
                if (index > -1) {
                    String argsStr = url.substring(index + 1);
                    String[] args = argsStr.split("&");
                    for (String arg : args) {
                        String[] kv = arg.split("=");
                        if (kv.length == 2) {
                            for (SecurityConfigIterm iterm : iterms) {
                                if (iterm.getConfig().getIsStart()) {
                                    Timer itermTimer = Constant.metrics.timer("ArgsSecurityFilter[" + iterm.getName() + "]");
                                    Timer.Context itermContext = itermTimer.time();
                                    try {
                                        Pattern pattern = Pattern.compile(iterm.getName());
                                        Matcher matcher = pattern.matcher(kv[1].toLowerCase());
                                        if (matcher.find()) {
                                            hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "Args", iterm.getName());
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
            }
        }
        return false;
    }
}
