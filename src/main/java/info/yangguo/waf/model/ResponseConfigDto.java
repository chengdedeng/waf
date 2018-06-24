package info.yangguo.waf.model;

import info.yangguo.waf.validator.CheckResponseConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ResponseConfigDto {
    @NotNull
    @ApiModelProperty(value = "response拦截器名称", required = true)
    private String filterName;
    @NotNull
    @CheckResponseConfig
    @ApiModelProperty(value = "response拦截器配置", required = true)
    private Config config;
}
