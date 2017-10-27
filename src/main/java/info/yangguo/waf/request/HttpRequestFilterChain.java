package info.yangguo.waf.request;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @author:杨果
 * @date:2017/4/11 上午11:32
 * <p>
 * Description:
 * <p>
 * 拦截器链
 */
public class HttpRequestFilterChain {
    public List<HttpRequestFilter> filters = new ArrayList<>();

    public HttpRequestFilterChain addFilter(HttpRequestFilter filter) {
        filters.add(filter);
        return this;
    }

    public ImmutablePair<Boolean, HttpRequestFilter> doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
        for (HttpRequestFilter filter : filters) {
            boolean result = filter.doFilter(originalRequest, httpObject, channelHandlerContext);
            if (result && filter.isBlacklist()) {
                return new ImmutablePair<>(filter.isBlacklist(), filter);
            } else if (result && !filter.isBlacklist()) {
                break;
            }
        }
        return new ImmutablePair<>(false, null);
    }
}
