package com.chinaredstar.waf;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

/**
 * @author:杨果
 * @date:2017/4/28 下午1:55
 *
 * Description:
 *
 */
public class HeaderHttpRequestFilter implements HttpRequestFilter {
    @Override
    public boolean doFilter(HttpRequest httpRequest) {
        String methodName = httpRequest.getMethod().name();
        String contentLength = httpRequest.headers().get("content-length");

        HttpPostRequestDecoder d = new HttpPostRequestDecoder(httpRequest);
        return false;
    }
}
