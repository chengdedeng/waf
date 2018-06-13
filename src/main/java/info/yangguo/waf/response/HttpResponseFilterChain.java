package info.yangguo.waf.response;

import info.yangguo.waf.model.Config;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author:杨果
 * @date:2017/4/11 下午4:45
 * <p>
 * Description:
 */
public class HttpResponseFilterChain {
    public List<HttpResponseFilter> filters = new ArrayList<>();

    public HttpResponseFilterChain() {
        filters.add(new ClickjackHttpResponseFilter());
    }

    public void doFilter(HttpRequest originalRequest, HttpResponse httpResponse, Map<String, Config> configs) {
        for (HttpResponseFilter filter : filters) {
            Config config = configs.get(filter.getClass().getName());
            if (config.getIsStart()) {
                filter.doFilter(originalRequest, httpResponse);
            }
        }
    }
}
