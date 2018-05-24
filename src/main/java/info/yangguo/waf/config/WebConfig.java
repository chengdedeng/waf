package info.yangguo.waf.config;

import io.atomix.cluster.Node;
import io.atomix.core.Atomix;
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
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.Filter;
import java.io.File;
import java.util.stream.Collectors;

@Configuration
@EnableSwagger2
public class WebConfig extends WebMvcConfigurerAdapter {
    @Autowired
    private ClusterProperties clusterProperties;

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

    @Bean("atomix")
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
                .addPartitionGroup(RaftPartitionGroup.builder("RaftPartitionGroup")
                        .withDataDirectory(new File(metadataDir + "/data/core"))
                        .withPartitionSize(3)
                        .withNumPartitions(1)
                        .build())
                .build();
        atomix.start().join();
        return atomix;
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("/api/.*"))//此处配置需注意，需要暴露什么配置什么
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("TITLE")
                .description("DESCRIPTION")
                .version("VERSION")
                .termsOfServiceUrl("https://github.com/chengdedeng/waf")
                .license("Apache License 2.0")
                .licenseUrl("https://en.wikipedia.org/wiki/Apache_License")
                .build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/api/**").excludePathPatterns("/api/user/**");
    }
}