package info.yangguo.waf.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import info.yangguo.waf.config.ClusterProperties;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.*;
import info.yangguo.waf.request.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import info.yangguo.waf.response.HttpResponseFilter;
import info.yangguo.waf.util.JsonUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ZkClusterService implements ClusterService {
    private static Logger LOGGER = LoggerFactory.getLogger(ZkClusterService.class);
    private static final String separator = "/";
    private static final String requestPath = "/waf/config/request";
    private static final String responsePath = "/waf/config/response";
    private static final String upstreamPath = "/waf/config/upstream";
    private static final String ENC = "UTF-8";

    private static CuratorFramework client;
    Map<String, RequestConfig> requestConfigMap = new HashMap<>();
    Map<String, Config> responseConfigMap = new HashMap<>();
    Map<String, WeightedRoundRobinScheduling> upstreamServerMap = new HashMap<>();

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


        TreeCache requestTreeCache = TreeCache.newBuilder(client, requestPath).setCacheData(true).build();
        requestTreeCache.start();
        requestTreeCache.getListenable().addListener((client, event) -> {
            if (TreeCacheEvent.Type.NODE_UPDATED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_ADDED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_REMOVED.equals(event.getType())
                    || TreeCacheEvent.Type.INITIALIZED.equals(event.getType())) {
                requestTreeCache.getCurrentChildren(requestPath).entrySet().stream().forEach(entry1 -> {
                    String filterName = entry1.getKey();
                    String filterPath = entry1.getValue().getPath();
                    boolean filterIsStart = Boolean.valueOf(new String(entry1.getValue().getData()));

                    Set<RequestConfig.Rule> rules = new HashSet<>();
                    requestTreeCache.getCurrentChildren(filterPath).entrySet().stream().forEach(entry2 -> {
                        String ruleRegex = null;
                        try {
                            ruleRegex = URLDecoder.decode(entry2.getKey(), ENC);
                        } catch (UnsupportedEncodingException e) {
                            LOGGER.error("Decode regex:[{}] ", entry2.getKey());
                        }
                        Boolean ruleIsStart = Boolean.valueOf(new String(entry2.getValue().getData()));
                        RequestConfig.Rule rule = new RequestConfig.Rule();
                        rule.setRegex(ruleRegex);
                        rule.setIsStart(ruleIsStart);
                        rules.add(rule);
                    });

                    RequestConfig requestConfig = new RequestConfig();
                    requestConfig.setIsStart(filterIsStart);
                    requestConfig.setRules(rules);

                    requestConfigMap.put(filterName, requestConfig);
                });
            }
        });

        TreeCache responseTreeCache = TreeCache.newBuilder(client, responsePath).setCacheData(true).build();
        responseTreeCache.start();
        responseTreeCache.getListenable().addListener((client, event) -> {
            if (TreeCacheEvent.Type.NODE_UPDATED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_ADDED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_REMOVED.equals(event.getType())
                    || TreeCacheEvent.Type.INITIALIZED.equals(event.getType())) {
                responseTreeCache.getCurrentChildren(responsePath).entrySet().stream().forEach(entry -> {
                    String filterName = entry.getKey();
                    Boolean filterIsStart = Boolean.valueOf(new String(entry.getValue().getData()));

                    Config config = new Config();
                    config.setIsStart(filterIsStart);

                    responseConfigMap.put(filterName, config);
                });
            }
        });

        if (client.checkExists().forPath(upstreamPath) == null) {
            client.create().forPath(upstreamPath);
        }
        TreeCache upstreamTreeCache = TreeCache.newBuilder(client, upstreamPath).setCacheData(true).build();
        upstreamTreeCache.start();
        upstreamTreeCache.getListenable().addListener((client, event) -> {
            if (TreeCacheEvent.Type.NODE_UPDATED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_ADDED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_REMOVED.equals(event.getType())
                    || TreeCacheEvent.Type.INITIALIZED.equals(event.getType())) {
                Set<String> hosts = Sets.newHashSet();
                upstreamTreeCache.getCurrentChildren(upstreamPath).entrySet().stream().forEach(entry1 -> {
                    String host = entry1.getKey();
                    String hostPath = entry1.getValue().getPath();
                    Boolean hostIsStart = Boolean.valueOf(new String(entry1.getValue().getData()));

                    List<Server> servers = Lists.newArrayList();
                    upstreamTreeCache.getCurrentChildren(hostPath).entrySet().stream().forEach(entry2 -> {
                        String[] serverInfo = entry2.getKey().split(":");
                        String ip = serverInfo[0];
                        int port = Integer.parseInt(serverInfo[1]);
                        ServerConfig serverConfig = (ServerConfig) JsonUtil.fromJson(new String(entry2.getValue().getData()), ServerConfig.class);

                        Server server = Server.builder()
                                .ip(ip)
                                .port(port)
                                .serverConfig(serverConfig)
                                .build();
                        servers.add(server);
                    });

                    WeightedRoundRobinScheduling weightedRoundRobinScheduling = new WeightedRoundRobinScheduling(servers, hostIsStart);
                    upstreamServerMap.put(host, weightedRoundRobinScheduling);
                    hosts.add(host);
                });
                //将已经删除的节点从upstreamServerMap中剔除掉
                upstreamServerMap.keySet().stream().collect(Collectors.toList()).stream().forEach(key -> {
                    if (!hosts.contains(key))
                        upstreamServerMap.remove(key);
                });
            }
        });
    }

    @Override
    public Map<String, RequestConfig> getRequestConfigs() {
        return requestConfigMap;
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
            String filterPath = requestPath + separator + filterName;
            if (client.checkExists().forPath(filterPath) == null) {
                LOGGER.warn("Path[{}] not exist.", filterPath);
            } else {
                String rulePath = filterPath + separator + URLEncoder.encode(regex, ENC);
                if (client.checkExists().forPath(rulePath) == null) {
                    client.create().forPath(rulePath, String.valueOf(isStart).getBytes());
                    LOGGER.info("Regex[{}]|Path[{}]|Data[{}] has been created.", regex, rulePath, isStart);
                } else {
                    client.setData().forPath(rulePath, String.valueOf(isStart).getBytes());
                    LOGGER.info("Regex[{}]|Path[{}]|Data[{}] has been set.", regex, rulePath, isStart);
                }
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
        return responseConfigMap;
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

    @Override
    public Map<String, WeightedRoundRobinScheduling> getUpstreamConfig() {
        return upstreamServerMap;
    }

    @Override
    public void setUpstream(Optional<String> hostOptional, Optional<Boolean> isStartOptional) {
        try {
            if (hostOptional.isPresent() && isStartOptional.isPresent()) {
                String path = upstreamPath + separator + hostOptional.get();
                if (client.checkExists().forPath(path) != null) {
                    client.setData().forPath(path, String.valueOf(isStartOptional.get()).getBytes());
                    LOGGER.info("Path[{}]|Data[{}] has been set.", path, isStartOptional.get());
                } else {
                    client.create().forPath(path, String.valueOf(isStartOptional.get()).getBytes());
                    LOGGER.info("Path[{}]|Data[{}] has been created.", path, isStartOptional.get());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUpstreamServer(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional, Optional<Boolean> isStartOptional, Optional<Integer> weightOptional) {
        try {
            if (hostOptional.isPresent() && ipOptional.isPresent() && portOptional.isPresent()) {
                String hostPath = upstreamPath + separator + hostOptional.get();
                if (client.checkExists().forPath(hostPath) == null) {
                    LOGGER.warn("Path[{}] not exist.", hostPath);
                } else {
                    String serverPath = hostPath + separator + ipOptional.get() + ":" + portOptional.get();
                    if (client.checkExists().forPath(serverPath) != null) {
                        ServerConfig serverConfig = (ServerConfig) JsonUtil.fromJson(new String(client.getData().forPath(serverPath)), ServerConfig.class);
                        isStartOptional.ifPresent(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean t) {
                                serverConfig.setIsStart(t);
                            }
                        });
                        weightOptional.ifPresent(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer t) {
                                serverConfig.setWeight(t);
                            }
                        });

                        String data = JsonUtil.toJson(serverConfig, false);
                        client.setData().forPath(serverPath, data.getBytes());
                        LOGGER.info("Path[{}]|Data[{}] has been set.", serverPath, data);
                    } else {
                        ServerConfig serverConfig = new ServerConfig();
                        serverConfig.setIsStart(isStartOptional.get());
                        serverConfig.setWeight(weightOptional.get());
                        String data = JsonUtil.toJson(serverConfig, false);
                        if (isStartOptional.isPresent() && weightOptional.isPresent()) {
                            client.create().forPath(serverPath, data.getBytes());
                            LOGGER.info("Path[{}]|Data[{}] has been created.", serverPath, data);
                        } else {
                            LOGGER.warn("Path[{}]|Data[{}] is incomplete.", serverPath, data);
                        }

                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteUpstream(Optional<String> hostOptional) {
        hostOptional.ifPresent(new Consumer<String>() {
            @Override
            public void accept(String s) {
                String hostPath = upstreamPath + separator + hostOptional.get();
                try {
                    if (client.checkExists().forPath(hostPath) != null) {
                        client.delete().deletingChildrenIfNeeded().forPath(hostPath);
                        LOGGER.info("Path[{}] has been deleted.", hostPath);
                    } else {
                        LOGGER.warn("Path[{}] not exist.", hostPath);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void deleteUpstreamServer(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional) {
        if (hostOptional.isPresent() && ipOptional.isPresent() && portOptional.isPresent()) {
            String serverPath = upstreamPath + separator + hostOptional.get() + separator + ipOptional.get() + ":" + portOptional.get();
            try {
                if (client.checkExists().forPath(serverPath) != null) {
                    client.delete().forPath(serverPath);
                    LOGGER.info("Path[{}] has been deleted.", serverPath);
                } else {
                    LOGGER.warn("Path[{}] not exist.", serverPath);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
