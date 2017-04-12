package com.chinaredstar.waf;

import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/4/11 下午2:06
 *
 * Description:
 *
 */
public class IpHttpRequestFilter implements HttpRequestFilter {
    @Override
    public boolean doFilter(HttpRequest httpRequest) {
        String host = httpRequest.headers().get("Host").replace(":", "_");
        String[] uris = httpRequest.getUri().split("\\?");
        String uri = uris[0];
        String xRealIP = httpRequest.headers().get("X-Real-IP");
//        if (null != xRealIP && !Constant.IpRateUtil.verify(host + uri, xRealIP)) {
        if (!Constant.IpRateUtil.verify(host + uri, "localhost")) {
            return true;
        }
        return false;
    }
}
