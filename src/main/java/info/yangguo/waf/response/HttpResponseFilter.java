package info.yangguo.waf.response;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * @author:杨果
 * @date:2017/4/11 上午11:29
 * <p>
 * Description:
 */
public interface HttpResponseFilter {
    HttpResponse doFilter(HttpRequest originalRequest, HttpResponse httpResponse);
}
