package info.yangguo.waf.service;

import info.yangguo.waf.config.ClusterProperties;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;
import info.yangguo.waf.request.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import info.yangguo.waf.util.JsonUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZkClusterService implements ClusterService {
    private static Logger logger = LoggerFactory.getLogger(ZkClusterService.class);
    private static final String separator = "/";
    private static final String requestPath = "/waf/config/request";
    private static final String responsePath = "/waf/config/response";

    private static CuratorFramework client;
    private static PathChildrenCache requestConfigs;
    private static PathChildrenCache responseConfigs;

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
            logger.error("zookeeper error:{}", message);
            e.printStackTrace();
        });
        client.getConnectionStateListenable().addListener((c, newState) -> {
            logger.info("zookeeper state:{}", newState);
        });
        client.start();

        requestConfigs = new PathChildrenCache(client, requestPath, true);
        requestConfigs.start();
        responseConfigs = new PathChildrenCache(client, responsePath, true);
        responseConfigs.start();


        createNode(requestPath + separator + ArgsHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + CCHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + CookieHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + IpHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + PostHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + FileHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + ScannerHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + UaHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + UrlHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + WIpHttpRequestFilter.class.getName(), "false".getBytes());
        createNode(requestPath + separator + WUrlHttpRequestFilter.class.getName(), "false".getBytes());

        createNode(responsePath + separator + ClickjackHttpResponseFilter.class.getName(), "false".getBytes());
    }

    @Override
    public Map<String, RequestConfig> getRequestConfigs() {
        Map<String, RequestConfig> requestConfigMap = new HashMap<>();
        List<ChildData> childDataList = requestConfigs.getCurrentData();
        childDataList.stream().forEach(childData -> {
            requestConfigMap.put(childData.getPath(), (RequestConfig) JsonUtil.fromJson(new String(childData.getData()), RequestConfig.class));
        });
        return requestConfigMap;
    }

    @Override
    public void setRequestSwitch(String filterName, Boolean isStart) {
        try {
            client.setData().forPath(requestPath + separator + filterName, String.valueOf(isStart).getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRequestRule(String filterName, String rule, Boolean isStart) {
        String rulePath = requestPath + separator + filterName + separator + rule;
        try {
            if (client.checkExists().forPath(rulePath) == null) {
                client.create().forPath(rulePath, String.valueOf(isStart).getBytes());
            } else {
                client.setData().forPath(rulePath, String.valueOf(isStart).getBytes());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteRequestRule(String filterName, String rule) {
        try {
            client.delete().forPath(requestPath + separator + filterName + separator + rule);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Config> getResponseConfigs() {
        Map<String, Config> configs = new HashMap<>();
        try {
            client.getChildren().forPath(responsePath).stream().forEach(path -> {
                Config config = new Config();
                try {
                    config.setIsStart(Boolean.valueOf(new String(client.getData().forPath(responsePath + separator + path))));
                } catch (Exception e) {
                    logger.warn("get responseConfig have exception:{}", e.getMessage());
                }
                configs.put(path, config);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configs;
    }

    @Override
    public void setResponseSwitch(String filterName, Boolean isStart) {
        try {
            client.setData().forPath(responsePath + separator + filterName, String.valueOf(isStart).getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createNode(String path, byte[] data) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            if (data != null)
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, data);
            else
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
        }
    }
}
