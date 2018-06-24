package info.yangguo.waf.model;

import info.yangguo.waf.validator.CheckRequestConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RequestConfigDto {
    @NotNull
    @ApiModelProperty(value = "request拦截器名称", required = true)
    private String filterName;
    @NotNull
    @CheckRequestConfig
    @ApiModelProperty(value = "request拦截器配置")
    private RequestConfig config;
}
