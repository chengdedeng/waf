package info.yangguo.waf.request;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.RateLimiter;

import info.yangguo.waf.Constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/5/12 上午11:37
 *
 * Description:
 * cc拦截
 */
public class CCHttpRequestFilter extends HttpRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CCHttpRequestFilter.class);
    private LoadingCache loadingCache;
    private static final ThreadLocal tl = new ThreadLocal();


    public CCHttpRequestFilter() {
        loadingCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.SECONDS)
                .removalListener(new RemovalListener() {
                    @Override
                    public void onRemoval(RemovalNotification notification) {
                        logger.debug("key:{} remove from cache", notification.getKey());
                    }
                })
                .build(new CacheLoader<String, RateLimiter>() {
                    @Override
                    public RateLimiter load(String key) throws Exception {
                        RateLimiter rateLimiter = RateLimiter.create(Integer.parseInt(Constant.wafConfs.get("waf.cc.rate")));
                        logger.debug("RateLimiter for key:{} have been created", key);
                        return rateLimiter;
                    }
                });
    }

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
        if (httpObject instanceof HttpRequest) {
            logger.debug("filter:{}", this.getClass().getName());
            String realIp = Constant.getRealIp((DefaultHttpRequest) httpObject, channelHandlerContext);
            RateLimiter rateLimiter = null;
            try {
                rateLimiter = (RateLimiter) loadingCache.get(realIp);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if (rateLimiter.tryAcquire()) {
                tl.set(false);
                return false;
            } else {
                hackLog(logger, Constant.getRealIp((DefaultHttpRequest) httpObject, channelHandlerContext), "cc", Constant.wafConfs.get("waf.cc.rate"));
                tl.set(true);
                return true;
            }
        }
        return false;
    }
}
