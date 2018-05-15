package info.yangguo.waf.config;

import io.atomix.cluster.Node;
import io.atomix.core.Atomix;
import io.atomix.core.map.ConsistentMap;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.Filter;
import java.io.File;
import java.util.stream.Collectors;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    @Autowired
    private ClusterProperties clusterProperties;
    @Autowired
    private Atomix atomix;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "PUT", "POST", "DELETE", "OPTIONS");
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public Filter characterEncodingFilter() {
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return characterEncodingFilter;
    }

    @Bean
    public MappingJackson2HttpMessageConverter converter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        return converter;
    }

    @Bean
    public StandardServletMultipartResolver getStandardServletMultipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean
    public Atomix getAtomix() {
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
        Atomix atomix = builder
                .withClusterName("WAF")
                .withDataDirectory(new File(metadataDir + "/data"))
                .addPartitionGroup(PrimaryBackupPartitionGroup.builder("data")
                        .withNumPartitions(3)
                        .build())
                .addPartitionGroup(RaftPartitionGroup.builder("core")
                        .withDataDirectory(new File(metadataDir + "/data/core"))
                        .withPartitionSize(3)
                        .withNumPartitions(3)
                        .build())
                .build();
        atomix.start().join();
        return atomix;
    }

    @Bean
    public ConsistentMap<String, String> getConsistentMap() {
        ConsistentMap<String, String> consistentMap = atomix.<String, String>consistentMapBuilder("WAF-CONFIG")
                .build();
        return consistentMap;

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/api/**").excludePathPatterns("/api/user/**");
    }
}