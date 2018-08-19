package info.yangguo.waf.request;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public class ForwardFilter implements RequestFilter {
    @Override
    public HttpResponse doFilter(HttpRequest originalRequest, HttpObject httpObject) {
        return null;
    }
}
