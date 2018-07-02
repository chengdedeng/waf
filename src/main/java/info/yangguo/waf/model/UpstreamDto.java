package info.yangguo.waf.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpstreamDto {
    @NotNull
    @ApiModelProperty(value = "host名称", required = true)
    private String host;
    @ApiModelProperty("host配置")
    private Boolean isStart;
    @ApiModelProperty("server配置")
    private List<ServerDto> servers;
    @Data
    @Builder
    public static class ServerDto{
        @ApiModelProperty("ip")
        String ip;
        @ApiModelProperty("port")
        int port;
        @ApiModelProperty("节点权重")
        private Integer weight;
        @ApiModelProperty("host配置")
        private Boolean isStart;
        @ApiModelProperty("server健康信息")
        private Boolean isHealth;
    }
}
