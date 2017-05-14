package com.chinaredstar.waf.request;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.RateLimiter;

import com.chinaredstar.waf.Constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

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
        CacheBuilder cacheBuilder = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.SECONDS)
                .removalListener(new RemovalListener() {
                    @Override
                    public void onRemoval(RemovalNotification notification) {
                        logger.debug("key:{} remove from cache", notification.getKey());
                    }
                })
                .recordStats();
        loadingCache = cacheBuilder.build(new CacheLoader<String, RateLimiter>() {
            @Override
            public RateLimiter load(String key) throws Exception {
                RateLimiter rateLimiter = RateLimiter.create(Integer.parseInt(Constant.wafConfs.get("waf.cc.rate")));
                logger.debug("RateLimiter for key:{} have been created", key);
                return rateLimiter;
            }
        });
    }

    @Override
    public boolean doFilter(HttpRequest httpRequest, ChannelHandlerContext channelHandlerContext) {
        logger.debug("filter:{}", this.getClass().getName());
        String realIp = Constant.getRealIp(httpRequest, channelHandlerContext);
        RateLimiter rateLimiter = null;
        try {
            rateLimiter = (RateLimiter) loadingCache.get(realIp);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if (httpRequest instanceof DefaultHttpRequest && !(httpRequest instanceof LastHttpContent)) {
            if (rateLimiter.tryAcquire()) {
                return false;
//            return true;
            } else {
                hackLog(logger, Constant.getRealIp(httpRequest, channelHandlerContext), "cc", Constant.wafConfs.get("waf.cc.rate"));
                return true;
            }
        }
        return false;
    }
}
