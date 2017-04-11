package com.chinaredstar.waf;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/4/11 上午11:32
 *
 * Description:
 *
 */
public class HttpRequestFilterChain {
    public List<HttpRequestFilter> filters = new ArrayList<>();

    public HttpRequestFilterChain addFilter(HttpRequestFilter filter) {
        filters.add(filter);
        return this;
    }

    public boolean doFilter(HttpRequest httpRequest) {
        for (HttpRequestFilter filter : filters) {
            boolean result = filter.doFilter(httpRequest);
            if (result) {
                return true;
            }
        }
        return false;
    }
}
