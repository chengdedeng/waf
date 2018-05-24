package info.yangguo.waf.config;

import info.yangguo.waf.request.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import io.atomix.core.Atomix;
import io.atomix.core.map.ConsistentMap;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ContextHolder implements ApplicationContextAware {
    public static ApplicationContext applicationContext;
    private static volatile ConsistentMap<String, Map> sessions;
    private static volatile ConsistentMap<String, Map> configs;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    //获取session map
    public static ConsistentMap<String, Map> getSessions() {
        if (sessions == null) {
            synchronized (ContextHolder.class) {
                if (sessions == null) {
                    Atomix atomix = (Atomix) applicationContext.getBean("atomix");
                    sessions = atomix.<String, Map>consistentMapBuilder("WAF-SESSION").withCacheEnabled()
                            .build();
                    return sessions;
                }
            }
        }
        return sessions;
    }

    //获取config map
    public static ConsistentMap<String, Map> getConfigs() {
        if (configs == null) {
            synchronized (ContextHolder.class) {
                if (configs == null) {
                    Atomix atomix = (Atomix) applicationContext.getBean("atomix");
                    ConsistentMap<String, Map> tmp = atomix.<String, Map>consistentMapBuilder("WAF-CONFIG").withCacheEnabled()
                            .build();
                    Map<String, Object> requestConfig = new HashMap<>();
                    requestConfig.put("isStart", false);
                    requestConfig.put("pattern", new HashMap<String, Boolean>());
                    Map<String, Object> responseConfig = new HashMap<>();
                    responseConfig.put("isStart", false);
                    tmp.putIfAbsent(ArgsHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(CCHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(CookieHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(IpHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(PostHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(FileHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(ScannerHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(UaHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(UrlHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(WIpHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(WUrlHttpRequestFilter.class.getName(), requestConfig);
                    tmp.putIfAbsent(ClickjackHttpResponseFilter.class.getName(), responseConfig);
                    configs = tmp;
                }
            }
        }
        return configs;
    }
}
