package info.yangguo.waf.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import info.yangguo.waf.config.ClusterProperties;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.*;
import info.yangguo.waf.request.security.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import info.yangguo.waf.response.HttpResponseFilter;
import info.yangguo.waf.util.JsonUtil;
import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ZkClusterService implements ClusterService {
    private static Logger LOGGER = LoggerFactory.getLogger(ZkClusterService.class);
    private static final String separator = "/";
    private static final String securityPath = "/waf/config/request/security";
    private static final String responsePath = "/waf/config/response";
    private static final String upstreamPath = "/waf/config/upstream";
    private static final String rewritePath = "/waf/config/rewrite";
    private static final String ENC = "UTF-8";

    private static CuratorFramework client;
    Map<String, SecurityConfig> requestConfigMap = Maps.newHashMap();
    Map<String, ResponseConfig> responseConfigMap = Maps.newHashMap();
    Map<String, WeightedRoundRobinScheduling> upstreamServerMap = Maps.newHashMap();
    Map<String, BasicConfig> rewriteConfigrMap = Maps.newHashMap();

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

        syncConfig();

        Arrays.stream(new Class[]{
                ArgsSecurityFilter.class,
                CCSecurityFilter.class,
                CookieSecurityFilter.class,
                IpSecurityFilter.class,
                PostSecurityFilter.class,
                FileSecurityFilter.class,
                ScannerSecurityFilter.class,
                UaSecurityFilter.class,
                UrlSecurityFilter.class,
                WIpSecurityFilter.class,
                WUrlSecurityFilter.class,
                ScriptSecurityFilter.class,
                ClickjackHttpResponseFilter.class
        }).forEach(filterClass -> {
            initFilter(filterClass);
        });


        if (client.checkExists().forPath(securityPath) == null) {
            client.create().forPath(securityPath);
        }
        TreeCache requestTreeCache = TreeCache.newBuilder(client, securityPath).setCacheData(true).build();
        requestTreeCache.start();
        requestTreeCache.getListenable().addListener((client, event) -> {
            if (TreeCacheEvent.Type.NODE_UPDATED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_ADDED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_REMOVED.equals(event.getType())
                    || TreeCacheEvent.Type.INITIALIZED.equals(event.getType())) {
                requestTreeCache.getCurrentChildren(securityPath).entrySet().stream().forEach(requestEntry -> {
                    String filterName = requestEntry.getKey();
                    String filterPath = requestEntry.getValue().getPath();
                    BasicConfig filterConfig = (BasicConfig) JsonUtil.fromJson(new String(requestEntry.getValue().getData()), BasicConfig.class);

                    List<SecurityConfigIterm> securityConfigIterms = Lists.newArrayList();
                    requestTreeCache.getCurrentChildren(filterPath).entrySet().stream().forEach(itermEntry -> {
                        String regex = null;
                        try {
                            regex = URLDecoder.decode(itermEntry.getKey(), ENC);
                        } catch (UnsupportedEncodingException e) {
                            LOGGER.error("Decode regex:[{}] ", itermEntry.getKey());
                        }
                        BasicConfig regexConfig = (BasicConfig) JsonUtil.fromJson(new String(itermEntry.getValue().getData()), BasicConfig.class);
                        securityConfigIterms.add(SecurityConfigIterm.builder().name(regex).config(regexConfig).build());
                    });

                    requestConfigMap.put(filterName, SecurityConfig.builder().filterName(filterName).config(filterConfig).securityConfigIterms(securityConfigIterms).build());
                });
                ConfigLocalCache.setRequestConfig(requestConfigMap);
            }
        });


        if (client.checkExists().forPath(responsePath) == null) {
            client.create().forPath(responsePath);
        }
        TreeCache responseTreeCache = TreeCache.newBuilder(client, responsePath).setCacheData(true).build();
        responseTreeCache.start();
        responseTreeCache.getListenable().addListener((client, event) -> {
            if (TreeCacheEvent.Type.NODE_UPDATED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_ADDED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_REMOVED.equals(event.getType())
                    || TreeCacheEvent.Type.INITIALIZED.equals(event.getType())) {
                responseTreeCache.getCurrentChildren(responsePath).entrySet().stream().forEach(entry -> {
                    String filterName = entry.getKey();
                    BasicConfig config = (BasicConfig) JsonUtil.fromJson(new String(entry.getValue().getData()), BasicConfig.class);
                    responseConfigMap.put(filterName, ResponseConfig.builder().filterName(filterName).config(config).build());
                });
                ConfigLocalCache.setResponseConfig(responseConfigMap);
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
                upstreamTreeCache.getCurrentChildren(upstreamPath).entrySet().stream().forEach(hostEntry -> {
                    String host = hostEntry.getKey();
                    String hostPath = hostEntry.getValue().getPath();
                    BasicConfig hostBasicConfig = (BasicConfig) JsonUtil.fromJson(new String(hostEntry.getValue().getData()), BasicConfig.class);

                    List<ServerConfig> serverConfigs = Lists.newArrayList();
                    upstreamTreeCache.getCurrentChildren(hostPath).entrySet().stream().forEach(serverEntry -> {
                        String[] serverInfo = serverEntry.getKey().split(":");
                        String ip = serverInfo[0];
                        int port = Integer.parseInt(serverInfo[1]);
                        ServerBasicConfig serverBasicConfig = (ServerBasicConfig) JsonUtil.fromJson(new String(serverEntry.getValue().getData()), ServerBasicConfig.class);

                        ServerConfig serverConfig = ServerConfig.builder()
                                .ip(ip)
                                .port(port)
                                .config(serverBasicConfig)
                                .build();
                        serverConfigs.add(serverConfig);
                    });

                    WeightedRoundRobinScheduling weightedRoundRobinScheduling = new WeightedRoundRobinScheduling(serverConfigs, hostBasicConfig);
                    upstreamServerMap.put(host, weightedRoundRobinScheduling);
                    hosts.add(host);
                });
                //将已经删除的节点从upstreamServerMap中剔除掉
                upstreamServerMap.keySet().stream().collect(Collectors.toList()).stream().forEach(key -> {
                    if (!hosts.contains(key))
                        upstreamServerMap.remove(key);
                });
                ConfigLocalCache.setUpstreamConfig(upstreamServerMap);
            }
        });


        if (client.checkExists().forPath(rewritePath) == null) {
            client.create().forPath(rewritePath);
        }
        TreeCache rewriteTreeCache = TreeCache.newBuilder(client, rewritePath).setCacheData(true).build();
        rewriteTreeCache.start();
        rewriteTreeCache.getListenable().addListener((client, event) -> {
            if (TreeCacheEvent.Type.NODE_UPDATED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_ADDED.equals(event.getType())
                    || TreeCacheEvent.Type.NODE_REMOVED.equals(event.getType())
                    || TreeCacheEvent.Type.INITIALIZED.equals(event.getType())) {
                Set<String> wafRoutes = Sets.newHashSet();
                rewriteTreeCache.getCurrentChildren(rewritePath).entrySet().stream().forEach(hostEntry -> {
                    String wafRoute = hostEntry.getKey();
                    BasicConfig hostBasicConfig = (BasicConfig) JsonUtil.fromJson(new String(hostEntry.getValue().getData()), BasicConfig.class);

                    rewriteConfigrMap.put(wafRoute, hostBasicConfig);
                    wafRoutes.add(wafRoute);
                });
                //将已经删除的节点从rewriteConfigrMap中剔除掉
                rewriteConfigrMap.keySet().stream().collect(Collectors.toList()).stream().forEach(key -> {
                    if (!wafRoutes.contains(key))
                        rewriteConfigrMap.remove(key);
                });
                ConfigLocalCache.setRewriteConfig(rewriteConfigrMap);
            }
        });
    }

    @Override
    public Map<String, SecurityConfig> getSecurityConfigs() {
        return requestConfigMap;
    }

    @Override
    public void setSecurityConfig(Optional<String> filterName, Optional<BasicConfig> config) {
        try {
            if (filterName.isPresent() && config.isPresent()) {
                String path = securityPath + separator + filterName.get();
                if (client.checkExists().forPath(path) != null) {
                    String data = JsonUtil.toJson(config.get(), false);
                    client.setData().forPath(path, data.getBytes());
                    LOGGER.info("Path[{}]|Data[{}] has been set.", path, data);
                } else {
                    LOGGER.warn("Path[{}] not exist.", path);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSecurityConfigIterm(Optional<String> filterName, Optional<String> iterm, Optional<BasicConfig> config) {
        try {
            if (filterName.isPresent() && iterm.isPresent() && config.isPresent()) {
                String filterPath = securityPath + separator + filterName.get();
                if (client.checkExists().forPath(filterPath) == null) {
                    LOGGER.warn("Path[{}] not exist.", filterPath);
                } else {
                    String rulePath = filterPath + separator + URLEncoder.encode(iterm.get(), ENC);
                    String data = JsonUtil.toJson(config.get(), false);
                    if (client.checkExists().forPath(rulePath) == null) {
                        client.create().forPath(rulePath, data.getBytes());
                        LOGGER.info("Path[{}]|Regex[{}]|Data[{}] has been created.", rulePath, iterm.get(), data);
                    } else {
                        client.setData().forPath(rulePath, data.getBytes());
                        LOGGER.info("Path[{}]|Regex[{}]|Data[{}] has been set.", rulePath, iterm.get(), data);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteSecurityConfigIterm(Optional<String> filterName, Optional<String> iterm) {
        try {
            if (filterName.isPresent() && iterm.isPresent()) {
                String itermPath = securityPath + separator + filterName.get() + separator + URLEncoder.encode(iterm.get(), ENC);
                if (client.checkExists().forPath(itermPath) != null) {
                    client.delete().forPath(itermPath);
                    LOGGER.info("Path[{}]|Regex[{}] has been deleted.", itermPath, iterm.get());
                } else {
                    LOGGER.warn("Path[{}]|Regex[{}] not exist.", itermPath, iterm.get());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, ResponseConfig> getResponseConfigs() {
        return responseConfigMap;
    }

    @Override
    public void setResponseConfig(Optional<String> filterName, Optional<BasicConfig> config) {
        try {
            if (filterName.isPresent() && config.isPresent()) {
                String path = responsePath + separator + filterName.get();
                if (client.checkExists().forPath(path) != null) {
                    String data = JsonUtil.toJson(config.get(), false);
                    client.setData().forPath(path, data.getBytes());
                    LOGGER.info("Path[{}]|Data[{}] has been set.", path, data);
                } else {
                    LOGGER.warn("Path[{}] not exist.", path);
                }
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
    public void setUpstreamConfig(Optional<String> hostOptional, Optional<BasicConfig> config) {
        try {
            if (hostOptional.isPresent() && config.isPresent()) {
                String path = upstreamPath + separator + hostOptional.get();
                String data = JsonUtil.toJson(config.get(), false);
                if (client.checkExists().forPath(path) != null) {
                    client.setData().forPath(path, data.getBytes());
                    LOGGER.info("Path[{}]|Data[{}] has been set.", path, data);
                } else {
                    client.create().forPath(path, data.getBytes());
                    LOGGER.info("Path[{}]|Data[{}] has been created.", path, data);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUpstreamServerConfig(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional, Optional<ServerBasicConfig> config) {
        try {
            if (hostOptional.isPresent() && ipOptional.isPresent() && portOptional.isPresent() && config.isPresent()) {
                String hostPath = upstreamPath + separator + hostOptional.get();
                if (client.checkExists().forPath(hostPath) == null) {
                    LOGGER.warn("Path[{}] not exist.", hostPath);
                } else {
                    String serverPath = hostPath + separator + ipOptional.get() + ":" + portOptional.get();
                    String data = JsonUtil.toJson(config.get(), false);
                    if (client.checkExists().forPath(serverPath) != null) {
                        client.setData().forPath(serverPath, data.getBytes());
                        LOGGER.info("Path[{}]|Data[{}] has been set.", serverPath, data);
                    } else {
                        client.create().forPath(serverPath, data.getBytes());
                        LOGGER.info("Path[{}]|Data[{}] has been created.", serverPath, data);
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

    @Override
    public Map<String, BasicConfig> getRewriteConfigs() {
        return rewriteConfigrMap;
    }

    @Override
    public void setRewriteConfig(Optional<String> wafRoute, Optional<BasicConfig> config) {
        try {
            if (wafRoute.isPresent() && config.isPresent()) {
                String path = rewritePath + separator + wafRoute.get();
                if (client.checkExists().forPath(path) == null) {
                    client.create().creatingParentsIfNeeded().forPath(path);
                }
                String data = JsonUtil.toJson(config.get(), false);
                client.setData().forPath(path, data.getBytes());
                LOGGER.info("Path[{}]|Data[{}] has been set.", path, data);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteRewrite(Optional<String> wafRoute) {
        wafRoute.ifPresent(new Consumer<String>() {
            @Override
            public void accept(String s) {
                String wafRoutePath = rewritePath + separator + wafRoute.get();
                try {
                    if (client.checkExists().forPath(wafRoutePath) != null) {
                        client.delete().deletingChildrenIfNeeded().forPath(wafRoutePath);
                        LOGGER.info("Path[{}] has been deleted.", wafRoutePath);
                    } else {
                        LOGGER.warn("Path[{}] not exist.", wafRoutePath);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void initFilter(Class filterClass) {
        String path;
        if (SecurityFilter.class.isAssignableFrom(filterClass)) {
            path = securityPath + separator + filterClass.getName();
        } else if (HttpResponseFilter.class.isAssignableFrom(filterClass)) {
            path = responsePath + separator + filterClass.getName();
        } else {
            throw new RuntimeException("Filter class:[" + filterClass.getName() + "] has error when to initialize filter.");
        }
        try {
            if (client.checkExists().forPath(path) == null) {
                String data = JsonUtil.toJson(BasicConfig.builder().isStart(false).build(), false);
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, data.getBytes());
                LOGGER.info("Path[{}]|Data[{}] has been initialized", path, data);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void syncConfig() {
        ConfigLocalCache
                .getRequestConfig()
                .entrySet()
                .stream()
                .filter(entry -> {
                    boolean pass = false;
                    try {
                        if (client.checkExists().forPath(securityPath) == null) {
                            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(securityPath + separator + entry.getKey());
                            pass = true;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("request config sync fail");
                    }
                    return pass;
                })
                .forEach(entry -> {
                    String filterName = entry.getKey();
                    SecurityConfig requestConfig = entry.getValue();
                    setSecurityConfig(Optional.of(filterName), Optional.of(requestConfig.getConfig()));
                    requestConfig.getSecurityConfigIterms().stream().forEach(itermConfig -> {
                        setSecurityConfigIterm(Optional.of(filterName), Optional.of(itermConfig.getName()), Optional.of(itermConfig.getConfig()));
                    });
                });
        ConfigLocalCache
                .getResponseConfig()
                .entrySet()
                .stream()
                .filter(entry -> {
                    boolean pass = false;
                    try {
                        if (client.checkExists().forPath(responsePath) == null) {
                            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(responsePath + separator + entry.getKey());
                            pass = true;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("response config sync fail");
                    }
                    return pass;
                })
                .forEach(entry -> {
                    String filterName = entry.getKey();
                    ResponseConfig responseConfig = entry.getValue();
                    setResponseConfig(Optional.of(filterName), Optional.of(responseConfig.getConfig()));
                });
        ConfigLocalCache
                .getUpstreamConfig()
                .entrySet()
                .stream()
                .filter(entry -> {
                    boolean pass = false;
                    try {
                        if (client.checkExists().forPath(upstreamPath) == null) {
                            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(upstreamPath + separator + entry.getKey());
                            pass = true;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("upstream config sync fail");
                    }
                    return pass;
                })
                .forEach(entry1 -> {
                    String host = entry1.getKey();
                    WeightedRoundRobinScheduling weightedRoundRobinScheduling = entry1.getValue();
                    setUpstreamConfig(Optional.of(host), Optional.of(weightedRoundRobinScheduling.getBasicConfig()));
                    weightedRoundRobinScheduling
                            .getServersMap()
                            .entrySet()
                            .stream()
                            .forEach(entry2 -> {
                                ServerConfig serverConfig = entry2.getValue();
                                setUpstreamServerConfig(Optional.of(host), Optional.of(serverConfig.getIp()), Optional.of(serverConfig.getPort()), Optional.of(serverConfig.getConfig()));
                            });
                });
        ConfigLocalCache
                .getRewriteConfig()
                .entrySet()
                .stream()
                .filter(entry -> {
                    boolean pass = false;
                    try {
                        if (client.checkExists().forPath(rewritePath) == null) {
                            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(rewritePath + separator + entry.getKey());
                            pass = true;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("rewrite config sync fail");
                    }
                    return pass;
                })
                .forEach(entry -> {
                    String wafRoute = entry.getKey();
                    BasicConfig basicConfig = entry.getValue();
                    setRewriteConfig(Optional.of(wafRoute), Optional.of(basicConfig));
                });
    }


    /**
     * 将配置缓存到本地，如果配置中心宕机，切换到新的配置中心时，会自动将本地缓存配置同步到新的配置中心，从而减少配置负担。
     */
    private static class ConfigLocalCache {
        private static String cachePath = System.getProperties().getProperty("user.home") + "/.waf";
        private static String requestCacheFile = cachePath + "/request-config.json";
        private static String responseCacheile = cachePath + "/response-config.json";
        private static String upstreamCacheFile = cachePath + "/upstream-config.json";
        private static String rewriteCacheFile = cachePath + "/rewrite-config.json";

        public static Map<String, SecurityConfig> getRequestConfig() {
            Map<String, SecurityConfig> config = Maps.newHashMap();
            File file = new File(requestCacheFile);
            if (file.exists()) {
                try {
                    config.putAll(JsonUtil.fromJson(FileUtils.readFileToString(file), new TypeReference<Map<String, SecurityConfig>>() {
                    }));
                } catch (Exception e) {
                    file.delete();
                    LOGGER.warn("format of request local config is incorrect", e);
                }
            }

            return config;
        }

        public static void setRequestConfig(Map<String, SecurityConfig> config) {
            try {
                FileUtils.write(new File(requestCacheFile), JsonUtil.toJson(config, true));
            } catch (IOException e) {
                LOGGER.error("request local cache setting is fail", e);
            }
        }

        public static Map<String, ResponseConfig> getResponseConfig() {
            Map<String, ResponseConfig> config = Maps.newHashMap();
            File file = new File(responseCacheile);
            if (file.exists()) {
                try {
                    config.putAll(JsonUtil.fromJson(FileUtils.readFileToString(file), new TypeReference<Map<String, ResponseConfig>>() {
                    }));
                } catch (Exception e) {
                    file.delete();
                    LOGGER.warn("format of response local config is incorrect", e);
                }
            }

            return config;
        }

        public static void setResponseConfig(Map<String, ResponseConfig> config) {
            try {
                FileUtils.write(new File(responseCacheile), JsonUtil.toJson(config, true));
            } catch (IOException e) {
                LOGGER.error("response local cache setting is fail", e);
            }
        }

        public static Map<String, WeightedRoundRobinScheduling> getUpstreamConfig() {
            Map<String, WeightedRoundRobinScheduling> config = Maps.newHashMap();
            File file = new File(upstreamCacheFile);
            if (file.exists()) {
                try {
                    config.putAll(JsonUtil.fromJson(FileUtils.readFileToString(file), new TypeReference<Map<String, WeightedRoundRobinScheduling>>() {
                    }));
                } catch (Exception e) {
                    file.delete();
                    LOGGER.warn("format of upstream local config is incorrect", e);
                }
            }

            return config;

        }

        public static void setUpstreamConfig(Map<String, WeightedRoundRobinScheduling> config) {
            try {
                FileUtils.write(new File(upstreamCacheFile), JsonUtil.toJson(config, true));
            } catch (IOException e) {
                LOGGER.error("upstream local cache setting is fail", e);
            }
        }

        public static Map<String, BasicConfig> getRewriteConfig() {
            Map<String, BasicConfig> config = Maps.newHashMap();
            File file = new File(rewriteCacheFile);
            if (file.exists()) {
                try {
                    config.putAll(JsonUtil.fromJson(FileUtils.readFileToString(file), new TypeReference<Map<String, BasicConfig>>() {
                    }));
                } catch (Exception e) {
                    file.delete();
                    LOGGER.warn("format of rewrite local config is incorrect", e);
                }
            }

            return config;

        }

        public static void setRewriteConfig(Map<String, BasicConfig> config) {
            try {
                FileUtils.write(new File(rewriteCacheFile), JsonUtil.toJson(config, true));
            } catch (IOException e) {
                LOGGER.error("rewrite local cache setting is fail", e);
            }
        }
    }
}
