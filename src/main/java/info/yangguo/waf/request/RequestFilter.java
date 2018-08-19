package info.yangguo.waf.request;

import io.netty.handler.codec.http.*;

public interface RequestFilter {
    HttpResponse doFilter(HttpRequest originalRequest, HttpObject httpObject) throws Exception;
}
