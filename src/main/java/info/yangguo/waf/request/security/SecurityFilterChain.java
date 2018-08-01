package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.model.SecurityConfig;
import info.yangguo.waf.service.ClusterService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;

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
public class SecurityFilterChain {
    public List<SecurityFilter> filters = new ArrayList<>();

    public SecurityFilterChain() {
        //要注意顺序，是从上向下执行的
        filters.add(new WIpSecurityFilter());
        filters.add(new IpSecurityFilter());
        filters.add(new CCSecurityFilter());
        filters.add(new ScannerSecurityFilter());
        filters.add(new WUrlSecurityFilter());
        filters.add(new UaSecurityFilter());
        filters.add(new UrlSecurityFilter());
        filters.add(new ArgsSecurityFilter());
        filters.add(new CookieSecurityFilter());
        filters.add(new PostSecurityFilter());
        filters.add(new FileSecurityFilter());
        filters.add(new ScriptSecurityFilter());
    }

    public ImmutablePair<Boolean, SecurityFilter> doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext channelHandlerContext, ClusterService clusterService) {
        for (SecurityFilter filter : filters) {
            Timer filterTimer = Constant.metrics.timer(filter.getClass().getName());
            Timer.Context filterContext = filterTimer.time();
            try {
                SecurityConfig config = clusterService.getRequestConfigs().get(filter.getClass().getName());
                if (config.getConfig().getIsStart()) {
                    boolean result = filter.doFilter(originalRequest, httpObject, channelHandlerContext, config.getSecurityConfigIterms());
                    if (result && filter.isBlacklist()) {
                        return new ImmutablePair<>(filter.isBlacklist(), filter);
                    } else if (result && !filter.isBlacklist()) {
                        break;
                    }
                }
            } finally {
                filterContext.stop();
            }
        }
        return new ImmutablePair<>(false, null);
    }
}
