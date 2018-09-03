package info.yangguo.waf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpstreamConfig implements Serializable {
    private static final long serialVersionUID = 1392103965835939457L;
    /**
     * upstream唯一标志
     */
    private String wafRoute;
    /**
     * 配置信息
     */
    private BasicConfig config;
    /**
     * 服务器列表
     */
    private List<ServerConfig> serverConfigs;
}
