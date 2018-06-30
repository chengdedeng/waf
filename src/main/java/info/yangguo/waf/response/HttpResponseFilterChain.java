package info.yangguo.waf.response;

import info.yangguo.waf.model.Config;
import info.yangguo.waf.service.ClusterService;
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

    public void doFilter(HttpRequest originalRequest, HttpResponse httpResponse, ClusterService clusterService) {
        for (HttpResponseFilter filter : filters) {
            Config config = clusterService.getResponseConfig(filter.getClass());
            if (config.getIsStart()) {
                filter.doFilter(originalRequest, httpResponse);
            }
        }
    }
}
