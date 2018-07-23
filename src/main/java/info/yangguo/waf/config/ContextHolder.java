package info.yangguo.waf.config;

import info.yangguo.waf.service.ClusterService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ContextHolder implements ApplicationContextAware {
    public static ApplicationContext applicationContext;
    private static ClusterService clusterService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static ClusterService getClusterService() {
        return clusterService;
    }

    public static void setClusterService(ClusterService clusterService) {
        ContextHolder.clusterService = clusterService;
    }
}
