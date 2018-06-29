package info.yangguo.waf.model;

import info.yangguo.waf.validator.CheckResponseConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseConfigDto {
    @NotNull
    @ApiModelProperty(value = "response拦截器名称", required = true)
    private String filterName;
    @NotNull
    @CheckResponseConfig
    @ApiModelProperty(value = "response拦截器配置", required = true)
    private Config config;
}
