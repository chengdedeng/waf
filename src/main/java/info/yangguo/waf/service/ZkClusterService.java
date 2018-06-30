package info.yangguo.waf.service;

import info.yangguo.waf.config.ClusterProperties;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;
import info.yangguo.waf.request.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import info.yangguo.waf.response.HttpResponseFilter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class ZkClusterService implements ClusterService {
    private static Logger LOGGER = LoggerFactory.getLogger(ZkClusterService.class);
    private static final String separator = "/";
    private static final String requestPath = "/waf/config/request";
    private static final String responsePath = "/waf/config/response";
    private static final String ENC = "UTF-8";

    private static CuratorFramework client;
    TreeCache requestTreeCache;
    TreeCache responseTreeCache;

    public ZkClusterService() throws Exception {
        ClusterProperties.ZkProperty zkProperty = ((ClusterProperties) ContextHolder.applicationContext.getBean("clusterProperties")).getZk();

        // these are reasonable arguments for the ExponentialBackoffRetry. The first
        // retry will wait 1 second - the second will wait up to 2 seconds - the
        // third will wait up to 4 seconds.
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // using the CuratorFrameworkFactory.builder() gives fine grained control
        // over creation options. See the CuratorFrameworkFactory.Builder javadoc
        // details
        client = CuratorFrameworkFactory.builder()
                .connectString(zkProperty.getConnectionString())
                .retryPolicy(retryPolicy)
                .build();


        client.getUnhandledErrorListenable().addListener((message, e) -> {
            LOGGER.error("zookeeper error:{}", message);
            e.printStackTrace();
        });
        client.getConnectionStateListenable().addListener((c, newState) -> {
            LOGGER.info("zookeeper state:{}", newState);
        });
        client.start();

        LOGGER.info("************************");
        Arrays.stream(new Class[]{
                ArgsHttpRequestFilter.class,
                CCHttpRequestFilter.class,
                CookieHttpRequestFilter.class,
                IpHttpRequestFilter.class,
                PostHttpRequestFilter.class,
                FileHttpRequestFilter.class,
                ScannerHttpRequestFilter.class,
                UaHttpRequestFilter.class,
                UrlHttpRequestFilter.class,
                WIpHttpRequestFilter.class,
                WUrlHttpRequestFilter.class,
                ClickjackHttpResponseFilter.class
        }).forEach(filterClass -> {
            initFilter(filterClass);
        });
        LOGGER.info("************************");


        requestTreeCache = TreeCache.newBuilder(client, requestPath).setCacheData(true).build();
        requestTreeCache.start();
        responseTreeCache = TreeCache.newBuilder(client, responsePath).setCacheData(true).build();
        responseTreeCache.start();
    }

    @Override
    public Map<String, RequestConfig> getRequestConfigs() {
        Map<String, RequestConfig> requestConfigMap = new HashMap<>();
        requestTreeCache.getCurrentChildren(requestPath).entrySet().stream().forEach(entry -> {
            String filterName = entry.getKey();
            String filterPath = entry.getValue().getPath();

            RequestConfig requestConfig = getRequestConfig(filterPath);

            requestConfigMap.put(filterName, requestConfig);
        });
        return requestConfigMap;
    }

    @Override
    public RequestConfig getRequestConfig(Class requestFileterClass) {
        String filterPath = requestPath + separator + requestFileterClass.getName();
        return getRequestConfig(filterPath);
    }

    private RequestConfig getRequestConfig(String filterPath) {
        boolean filterIsStart = Boolean.valueOf(new String(requestTreeCache.getCurrentData(filterPath).getData()));
        Set<RequestConfig.Rule> rules = new HashSet<>();
        requestTreeCache.getCurrentChildren(filterPath).entrySet().stream().forEach(entry -> {
            String ruleRegex = null;
            try {
                ruleRegex = URLDecoder.decode(entry.getKey(), ENC);
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Decode regex:[{}] ", entry.getKey());
            }
            Boolean ruleIsStart = Boolean.valueOf(new String(entry.getValue().getData()));
            RequestConfig.Rule rule = new RequestConfig.Rule();
            rule.setRegex(ruleRegex);
            rule.setIsStart(ruleIsStart);
            rules.add(rule);
        });

        RequestConfig requestConfig = new RequestConfig();
        requestConfig.setIsStart(filterIsStart);
        requestConfig.setRules(rules);
        return requestConfig;
    }

    @Override
    public void setRequestSwitch(String filterName, Boolean isStart) {
        try {
            String path = requestPath + separator + filterName;
            if (client.checkExists().forPath(path) != null) {
                client.setData().forPath(path, String.valueOf(isStart).getBytes());
                LOGGER.info("Path[{}]|Data[{}] has been set.", path, isStart);
            } else {
                LOGGER.warn("Path[{}] not exist.", path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRequestRule(String filterName, String regex, Boolean isStart) {
        try {
            String rulePath = requestPath + separator + filterName + separator + URLEncoder.encode(regex, ENC);
            if (client.checkExists().forPath(rulePath) == null) {
                client.create().forPath(rulePath, String.valueOf(isStart).getBytes());
                LOGGER.info("Regex[{}]|Path[{}]|Data[{}] has been created.", regex, rulePath, isStart);
            } else {
                client.setData().forPath(rulePath, String.valueOf(isStart).getBytes());
                LOGGER.info("Regex[{}]|Path[{}]|Data[{}] has been set.", regex, rulePath, isStart);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteRequestRule(String filterName, String regex) {
        try {
            String rulePath = requestPath + separator + filterName + separator + URLEncoder.encode(regex, ENC);
            if (client.checkExists().forPath(rulePath) != null) {
                client.delete().forPath(rulePath);
                LOGGER.info("Regex[{}]|Path[{}] has been deleted.", regex, rulePath);
            } else {
                LOGGER.warn("Regex[{}]|Path[{}] not exist.", regex, rulePath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Config> getResponseConfigs() {
        Map<String, Config> responseConfigMap = new HashMap<>();
        responseTreeCache.getCurrentChildren(responsePath).entrySet().stream().forEach(entry -> {
            String filterName = entry.getKey();
            Boolean filterIsStart = Boolean.valueOf(new String(entry.getValue().getData()));

            Config config = new Config();
            config.setIsStart(filterIsStart);

            responseConfigMap.put(filterName, config);
        });
        return responseConfigMap;
    }

    @Override
    public Config getResponseConfig(Class responseFilterClass) {
        String filterPath = responsePath + separator + responseFilterClass.getName();
        boolean isStart = Boolean.valueOf(new String(responseTreeCache.getCurrentData(filterPath).getData()));
        Config config = new Config();
        config.setIsStart(isStart);
        return config;
    }

    @Override
    public void setResponseSwitch(String filterName, Boolean isStart) {
        try {
            String path = responsePath + separator + filterName;
            if (client.checkExists().forPath(path) != null) {
                client.setData().forPath(path, String.valueOf(isStart).getBytes());
                LOGGER.info("Path[{}]|Data[{}] has been set.", path, isStart);
            } else {
                LOGGER.warn("Path[{}] not exist.", path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initFilter(Class filterClass) {
        String path;
        if (HttpRequestFilter.class.isAssignableFrom(filterClass)) {
            path = requestPath + separator + filterClass.getName();
        } else if (HttpResponseFilter.class.isAssignableFrom(filterClass)) {
            path = responsePath + separator + filterClass.getName();
        } else {
            throw new RuntimeException("Filter class:[" + filterClass.getName() + "] has error when to initialize filter.");
        }
        try {
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, "false".getBytes());
                LOGGER.info("Path[{}]|Data[{}] has been initialized", path, false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
