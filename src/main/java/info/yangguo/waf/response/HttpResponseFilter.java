package info.yangguo.waf.response;

import info.yangguo.waf.model.ResponseConfig;
import info.yangguo.waf.service.ClusterService;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author:杨果
 * @date:2017/4/11 下午4:45
 * <p>
 * Description:
 */
public class HttpResponseFilter {
    public List<ResponseProcess> filters = new ArrayList<>();

    public HttpResponseFilter() {
        filters.add(new ClickjackResponseProcess());
    }

    public void doFilter(HttpRequest originalRequest, HttpResponse httpResponse, ClusterService clusterService) {
        for (ResponseProcess filter : filters) {
            ResponseConfig responseConfig = clusterService.getResponseConfigs().get(filter.getClass().getName());
            if (responseConfig.getConfig().getIsStart()) {
                filter.doFilter(originalRequest, httpResponse);
            }
        }
    }
}
