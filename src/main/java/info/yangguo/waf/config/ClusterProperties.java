/*
 * Copyright 2018-present yangguo@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.yangguo.waf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("clusterProperties")
@PropertySource("classpath:cluster.properties")
@ConfigurationProperties(prefix = "waf")
@Data
public class ClusterProperties {
    private AtomixProperty atomix;
    private ZkProperty zk;
    @Data
    public static class AtomixProperty {
        private String id;
        private String metadataDir;
        private List<ClusterNode> node;

    }
    @Data
    public static class ZkProperty {
        private String connectionString;
    }

    @Data
    public static class ClusterNode {
        private String id;
        private String ip;
        private int socket_port;
    }
}
