package info.yangguo.waf.dto;

import info.yangguo.waf.validator.Exist;
import info.yangguo.waf.validator.NotExist;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpstreamServerConfigDto {
    @NotEmpty
    @Pattern(regexp = ".*_\\d{1,5}")
    @ApiModelProperty(value = "X-Waf-Host-Port，路由标志。", required = true)
    private String wafHostPort;
    @NotEmpty
    @ApiModelProperty(value = "IP地址。", required = true)
    @Pattern(regexp = "(^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$)|(^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$)")
    private String ip;
    @Min(1)
    @Max(65535)
    @NotNull
    @ApiModelProperty(value = "端口。", required = true)
    private Integer port;
    @NotNull(groups = Exist.class)
    @Null(groups = NotExist.class)
    @ApiModelProperty(value = "开关，true启用，false关闭。")
    private Boolean isStart;
    @Min(value = 1, groups = Exist.class)
    @NotNull(groups = Exist.class)
    @Null(groups = NotExist.class)
    @ApiModelProperty("权重。")
    private Integer weight;
}
