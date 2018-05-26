package info.yangguo.waf.config;

import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;
import info.yangguo.waf.request.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import io.atomix.core.Atomix;
import io.atomix.core.map.ConsistentMap;
import io.atomix.utils.serializer.KryoNamespace;
import io.atomix.utils.serializer.KryoNamespaces;
import io.atomix.utils.serializer.Serializer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;

@Component
public class ContextHolder implements ApplicationContextAware {
    public static ApplicationContext applicationContext;
    private static volatile ConsistentMap<String, Map> sessions;
    private static volatile ConsistentMap<String, Config> configs;

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
    public static ConsistentMap<String, Config> getConfigs() {
        if (configs == null) {
            synchronized (ContextHolder.class) {
                if (configs == null) {
                    Atomix atomix = (Atomix) applicationContext.getBean("atomix");
                    KryoNamespace.Builder kryoBuilder = KryoNamespace.builder()
                            .register(KryoNamespaces.BASIC)
                            .register(Config.class)
                            .register(RequestConfig.Rule.class)
                            .register(RequestConfig.class);
                    ConsistentMap<String, Config> tmp = atomix
                            .<String, Config>consistentMapBuilder("WAF-CONFIG")
                            .withSerializer(Serializer.using(kryoBuilder.build()))
                            .withCacheEnabled()
                            .build();
                    RequestConfig requestConfig = new RequestConfig();
                    requestConfig.setRules(new HashSet<>());
                    requestConfig.setIsStart(false);
                    Config responseConfig = new Config();
                    responseConfig.setIsStart(false);
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
