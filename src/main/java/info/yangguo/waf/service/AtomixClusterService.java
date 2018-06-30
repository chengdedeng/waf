package info.yangguo.waf.service;

import info.yangguo.waf.config.ClusterProperties;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;
import info.yangguo.waf.request.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import io.atomix.cluster.Node;
import io.atomix.core.Atomix;
import io.atomix.core.map.ConsistentMap;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.utils.serializer.KryoNamespace;
import io.atomix.utils.serializer.KryoNamespaces;
import io.atomix.utils.serializer.Serializer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class AtomixClusterService implements ClusterService {
    private Atomix atomix;
    private static ConsistentMap<String, RequestConfig> requestConfigs;
    private static ConsistentMap<String, Config> responseConfigs;

    public AtomixClusterService() {
        ClusterProperties.AtomixProperty clusterProperties = ((ClusterProperties) ContextHolder.applicationContext.getBean("clusterProperties")).getAtomix();
        Atomix.Builder builder = Atomix.builder();
        clusterProperties.getNode().stream().forEach(clusterNode -> {
            if (clusterNode.getId().equals(clusterProperties.getId())) {
                builder
                        .withLocalNode(Node.builder(clusterNode.getId())
                                .withType(Node.Type.CORE)
                                .withAddress(clusterNode.getIp(), clusterNode.getSocket_port())
                                .build());
            }
        });

        builder.withNodes(clusterProperties.getNode().stream().map(clusterNode -> {
            return Node
                    .builder(clusterNode.getId())
                    .withType(Node.Type.CORE)
                    .withAddress(clusterNode.getIp(), clusterNode.getSocket_port()).build();
        }).collect(Collectors.toList()));
        String metadataDir = null;
        if (clusterProperties.getMetadataDir().startsWith("/")) {
            metadataDir = clusterProperties.getMetadataDir() + "/" + clusterProperties.getId();
        } else {
            metadataDir = FileUtils.getUserDirectoryPath() + "/" + clusterProperties.getMetadataDir() + "/" + clusterProperties.getId();
        }
        atomix = builder
                .withClusterName("WAF")
                .withDataDirectory(new File(metadataDir + "/data"))
                .addPartitionGroup(RaftPartitionGroup.builder("RaftPartitionGroup")
                        .withDataDirectory(new File(metadataDir + "/data/core"))
                        .withPartitionSize(3)
                        .withNumPartitions(1)
                        .build())
                .build();
        atomix.start().join();

        KryoNamespace.Builder kryoBuilder = KryoNamespace.builder()
                .register(KryoNamespaces.BASIC)
                .register(Config.class)
                .register(RequestConfig.Rule.class)
                .register(RequestConfig.class);

        requestConfigs = atomix
                .<String, RequestConfig>consistentMapBuilder("WAF-CONFIG-REQUEST")
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withCacheEnabled()
                .build();
        responseConfigs = atomix
                .<String, Config>consistentMapBuilder("WAF-CONFIG-RESPONSE")
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withCacheEnabled()
                .build();

        RequestConfig requestConfig = new RequestConfig();
        requestConfig.setRules(new HashSet<>());
        requestConfig.setIsStart(false);
        Config responseConfig = new Config();
        responseConfig.setIsStart(false);

        requestConfigs.putIfAbsent(ArgsHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(CCHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(CookieHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(IpHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(PostHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(FileHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(ScannerHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(UaHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(UrlHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(WIpHttpRequestFilter.class.getName(), requestConfig);
        requestConfigs.putIfAbsent(WUrlHttpRequestFilter.class.getName(), requestConfig);

        responseConfigs.putIfAbsent(ClickjackHttpResponseFilter.class.getName(), responseConfig);
    }

    @Override
    public Map<String, RequestConfig> getRequestConfigs() {
        return requestConfigs.asJavaMap();
    }

    @Override
    public void setRequestSwitch(String filterName, Boolean isStart) {
        RequestConfig requestConfig = requestConfigs.get(filterName).value();
        requestConfig.setIsStart(isStart);
        requestConfigs.put(filterName, requestConfig);
    }

    @Override
    public void setRequestRule(String filterName, String rule, Boolean isStart) {
        RequestConfig requestConfig = requestConfigs.get(filterName).value();
        requestConfig.getRules().stream().anyMatch(ruleTmp -> {
            if (ruleTmp.getRegex().equals(rule)) {
                ruleTmp.setIsStart(isStart);
                requestConfigs.put(filterName, requestConfig);
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public void deleteRequestRule(String filterName, String rule) {
        RequestConfig requestConfig = requestConfigs.get(filterName).value();
        requestConfig.getRules().stream().anyMatch(ruleTmp -> {
            if (ruleTmp.getRegex().equals(rule)) {
                requestConfig.getRules().remove(ruleTmp);
                requestConfigs.put(filterName, requestConfig);
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public Map<String, Config> getResponseConfigs() {
        return responseConfigs.asJavaMap();
    }


    @Override
    public void setResponseSwitch(String filterName, Boolean isStart) {
        Config config = responseConfigs.get(filterName).value();
        config.setIsStart(isStart);
        responseConfigs.put(filterName, config);
    }
}
