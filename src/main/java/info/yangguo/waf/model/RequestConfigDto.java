package info.yangguo.waf.model;

import info.yangguo.waf.validator.CheckRequestConfig;
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
public class RequestConfigDto {
    @NotNull
    @ApiModelProperty(value = "request拦截器名称", required = true)
    private String filterName;
    @NotNull
    @CheckRequestConfig
    @ApiModelProperty(value = "request拦截器配置")
    private RequestConfig config;
}
