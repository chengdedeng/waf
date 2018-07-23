package info.yangguo.waf.response;

import info.yangguo.waf.Constant;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * @author:杨果
 * @date:2017/4/11 下午4:39
 * <p>
 * Description:
 */
public class ClickjackHttpResponseFilter implements HttpResponseFilter {
    @Override
    public HttpResponse doFilter(HttpRequest originalRequest, HttpResponse httpResponse) {
        httpResponse.headers().add("X-FRAME-OPTIONS", Constant.X_Frame_Option);
        return httpResponse;
    }
}
