package info.yangguo.waf.response;

import io.atomix.core.map.ConsistentMap;
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

    public void doFilter(HttpRequest originalRequest, HttpResponse httpResponse, ConsistentMap<String, Map> configs) {
        for (HttpResponseFilter filter : filters) {
            Map<String, Object> config = configs.asJavaMap().get(filter.getClass().getName());
            if ((boolean) config.get("isStart")) {
                filter.doFilter(originalRequest, httpResponse);
            }
        }
    }
}
