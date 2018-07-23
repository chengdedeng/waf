package info.yangguo.waf.service;

import com.google.common.collect.Lists;
import info.yangguo.waf.config.ClusterProperties;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.*;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomixClusterService implements ClusterService {
    private Atomix atomix;
    private static ConsistentMap<String, RequestConfig> requestConfigs;
    private static ConsistentMap<String, ResponseConfig> responseConfigs;

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
                .register(BasicConfig.class)
                .register(ItermConfig.class)
                .register(RequestConfig.class)
                .register(ServerBasicConfig.class);

        requestConfigs = atomix
                .<String, RequestConfig>consistentMapBuilder("WAF-CONFIG-REQUEST")
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withCacheEnabled()
                .build();
        responseConfigs = atomix
                .<String, ResponseConfig>consistentMapBuilder("WAF-CONFIG-RESPONSE")
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withCacheEnabled()
                .build();

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
                WUrlHttpRequestFilter.class
        }).forEach(filterClass -> {
            RequestConfig requestConfig = new RequestConfig();
            requestConfig.setFilterName(filterClass.getName());
            requestConfig.setItermConfigs(Lists.newArrayList());
            BasicConfig basicConfig = new BasicConfig();
            basicConfig.setIsStart(false);
            requestConfig.setConfig(basicConfig);
            requestConfigs.putIfAbsent(filterClass.getName(), requestConfig);
        });

        Arrays.stream(new Class[]{
                ClickjackHttpResponseFilter.class
        }).forEach(filterClass -> {
            ResponseConfig responseConfig = new ResponseConfig();
            responseConfig.setFilterName(filterClass.getName());
            BasicConfig config = new BasicConfig();
            config.setIsStart(false);
            responseConfig.setConfig(config);
            responseConfigs.putIfAbsent(filterClass.getName(), responseConfig);
        });
    }

    @Override
    public Map<String, RequestConfig> getRequestConfigs() {
        return requestConfigs.asJavaMap();
    }

    @Override
    public void setRequestConfig(Optional<String> filterName, Optional<BasicConfig> config) {
        if (filterName.isPresent() && config.isPresent()) {
            RequestConfig requestConfig = requestConfigs.get(filterName.get()).value();
            requestConfig.setConfig(config.get());
            requestConfigs.put(filterName.get(), requestConfig);
        }
    }

    @Override
    public void setRequestItermConfig(Optional<String> filterName, Optional<String> iterm, Optional<BasicConfig> config) {
        if (filterName.isPresent() && iterm.isPresent() && config.isPresent()) {
            RequestConfig requestConfig = requestConfigs.get(filterName.get()).value();
            requestConfig.getItermConfigs().stream().anyMatch(ruleTmp -> {
                if (ruleTmp.getName().equals(iterm.get())) {
                    ruleTmp.setConfig(config.get());
                    requestConfigs.put(filterName.get(), requestConfig);
                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    @Override
    public void deleteRequestIterm(Optional<String> filterName, Optional<String> iterm) {
        if (filterName.isPresent() && iterm.isPresent()) {
            RequestConfig requestConfig = requestConfigs.get(filterName.get()).value();
            requestConfig.getItermConfigs().stream().anyMatch(ruleTmp -> {
                if (ruleTmp.getName().equals(iterm.get())) {
                    requestConfig.getItermConfigs().remove(ruleTmp);
                    requestConfigs.put(filterName.get(), requestConfig);
                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    @Override
    public Map<String, ResponseConfig> getResponseConfigs() {
        return responseConfigs.asJavaMap();
    }


    @Override
    public void setResponseConfig(Optional<String> filterName, Optional<BasicConfig> config) {
        if (filterName.isPresent() && config.isPresent()) {
            ResponseConfig responseConfig = responseConfigs.get(filterName.get()).value();
            responseConfig.setConfig(config.get());
            responseConfigs.put(filterName.get(), responseConfig);
        }
    }

    @Override
    public Map<String, WeightedRoundRobinScheduling> getUpstreamConfig() {
        //todo
        return null;
    }

    @Override
    public void setUpstreamConfig(Optional<String> hostOptional, Optional<BasicConfig> hostConfig) {
        //todo
    }

    @Override
    public void setUpstreamServerConfig(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional, Optional<ServerBasicConfig> serverConfig) {
        //todo
    }

    @Override
    public void deleteUpstream(Optional<String> hostOptional) {
        //todo
    }

    @Override
    public void deleteUpstreamServer(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional) {
        //todo
    }
}
