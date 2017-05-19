package info.yangguo.waf.request;

import info.yangguo.waf.Constant;
import info.yangguo.waf.util.ConfUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/5/11 上午9:45
 *
 * Description:
 *
 * URL参数黑名单参数拦截
 */
public class ArgsHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ArgsHttpRequestFilter.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            HttpRequest httpRequest = (HttpRequest) httpObject;
            int index = httpRequest.getUri().indexOf("?");
            if (index > -1) {
                String argsStr = httpRequest.getUri().substring(index + 1);
                String[] args = argsStr.split("&");
                for (String arg : args) {
                    String[] kv = arg.split("=");
                    if (kv.length == 2) {
                        for (Pattern pat : ConfUtil.getPattern(FilterType.ARGS.name())) {
                            Matcher matcher = pat.matcher(kv[1]);
                            if (matcher.find()) {
                                hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), FilterType.ARGS.name(), pat.toString());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
