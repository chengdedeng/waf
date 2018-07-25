package info.yangguo.waf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerConfig implements Serializable {
    private static final long serialVersionUID = 7426117635312217242L;
    /**
     * 服务器IP
     */
    private String ip;
    /**
     * 服务器端口
     */
    private Integer port;
    /**
     * 服务器配置信息
     */
    public ServerBasicConfig config;
    /**
     * 健康标记
     */
    private Boolean isHealth;
}
