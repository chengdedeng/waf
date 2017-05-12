package com.chinaredstar.waf.response;

import com.chinaredstar.waf.Constant;

import io.netty.handler.codec.http.HttpResponse;

/**
 * @author:杨果
 * @date:2017/4/11 下午4:39
 *
 * Description:
 *
 */
public class ClickjackHttpResponseFilter implements HttpResponseFilter {
    @Override
    public HttpResponse doFilter(HttpResponse httpResponse) {
        httpResponse.headers().add("X-FRAME-OPTIONS", Constant.X_Frame_Option);
        return httpResponse;
    }
}
