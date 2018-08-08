package info.yangguo.waf.request;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.model.ItermConfig;
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
 * <p>
 * 此处最好别用URI匹配正则，最好拆成NameValuePair一一匹配，这样精准一点。
 * 例如设置了一个<script>拦截，如果参数为a=中国&b=<script>&c=!@$%^&*()_+{}|就拦截失败
 */
public class ArgsHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ArgsHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, List<ItermConfig> iterms) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            int index = httpRequest.uri().indexOf("?");
            if (index > -1) {
                String argsStr = httpRequest.uri().substring(index + 1);
                List<NameValuePair> args = URLEncodedUtils.parse(argsStr, UTF_8);
                for (NameValuePair arg : args) {
                    for (ItermConfig iterm : iterms) {
                        if (iterm.getConfig().getIsStart()) {
                            Timer itermTimer = Constant.metrics.timer("ArgsHttpRequestFilter[" + iterm.getName() + "]");
                            Timer.Context itermContext = itermTimer.time();
                            try {
                                Pattern pattern = Pattern.compile(iterm.getName());
                                Matcher matcher = pattern.matcher(arg.getName().toLowerCase() + "=" + arg.getValue().toLowerCase());
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
        return false;
    }
}
