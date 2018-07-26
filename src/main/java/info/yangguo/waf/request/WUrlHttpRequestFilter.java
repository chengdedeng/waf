package info.yangguo.waf.request;

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
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, List<ItermConfig> iterms) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String url;
            int index = httpRequest.uri().indexOf("?");
            if (index > -1) {
                url = httpRequest.uri().substring(0, index);
            } else {
                url = httpRequest.uri();
            }
            for (ItermConfig iterm : iterms) {
                if (iterm.getConfig().getIsStart()) {
                    Pattern pattern = Pattern.compile(iterm.getName());
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "WUrl", iterm.getName());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}