package info.yangguo.waf.request;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.SecurityConfig;
import info.yangguo.waf.request.security.*;
import info.yangguo.waf.util.ResponseUtil;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author:杨果
 * @date:2017/4/11 上午11:32
 * <p>
 * Description:
 * <p>
 * 拦截器链
 */
public class SecurityFilter implements RequestFilter {
    public List<Security> filters = new ArrayList<>();

    public SecurityFilter() {
        //要注意顺序，是从上向下执行的
        filters.add(new WIpSecurity());
        filters.add(new IpSecurity());
        filters.add(new CCSecurity());
        filters.add(new ScannerSecurity());
        filters.add(new WUrlSecurity());
        filters.add(new UaSecurity());
        filters.add(new UrlSecurity());
        filters.add(new ArgsSecurity());
        filters.add(new CookieSecurity());
        filters.add(new PostSecurity());
        filters.add(new FileSecurity());
        filters.add(new ScriptSecurity());
    }

    @Override
    public HttpResponse doFilter(HttpRequest originalRequest, HttpObject httpObject) {
        HttpResponse httpResponse = null;
        Pair<Boolean, Security> pair = null;
        for (Security filter : filters) {
            Timer filterTimer = Constant.metrics.timer(filter.getClass().getName());
            Timer.Context filterContext = filterTimer.time();
            try {
                SecurityConfig config = ContextHolder.getClusterService().getSecurityConfigs().get(filter.getClass().getName());
                if (config.getConfig().getIsStart()) {
                    boolean result = filter.doFilter(originalRequest, httpObject, config.getSecurityConfigItems());
                    if (result && filter.isBlacklist()) {
                        pair = new ImmutablePair<>(filter.isBlacklist(), filter);
                        break;
                    } else if (result && !filter.isBlacklist()) {
                        break;
                    }
                }
            } finally {
                filterContext.stop();
            }
        }

        if (pair != null) {
            if (pair.getLeft()) {
                if (pair.getRight() instanceof CCSecurity)
                    httpResponse = ResponseUtil.createResponse(HttpResponseStatus.SERVICE_UNAVAILABLE, originalRequest, null);
                else
                    httpResponse = ResponseUtil.createResponse(HttpResponseStatus.FORBIDDEN, originalRequest, null);
            }
        }
        return httpResponse;
    }
}
