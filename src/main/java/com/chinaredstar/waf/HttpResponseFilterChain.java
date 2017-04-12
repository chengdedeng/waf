package com.chinaredstar.waf;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.http.HttpResponse;

/**
 * @author:杨果
 * @date:2017/4/11 下午4:45
 *
 * Description:
 *
 */
public class HttpResponseFilterChain {
    public List<HttpResponseFilter> filters = new ArrayList<>();

    public HttpResponseFilterChain addFilter(HttpResponseFilter filter) {
        filters.add(filter);
        return this;
    }

    public void doFilter(HttpResponse httpResponse) {
        for (HttpResponseFilter filter : filters) {
            filter.doFilter(httpResponse);
        }
    }
}
