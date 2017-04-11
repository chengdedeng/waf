package com.chinaredstar.waf;

import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/4/11 上午11:28
 *
 * Description:
 *
 */
public interface HttpRequestFilter {
    boolean doFilter(HttpRequest httpRequest);
}
