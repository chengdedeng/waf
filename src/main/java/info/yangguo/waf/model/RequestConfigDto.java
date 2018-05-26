package info.yangguo.waf.model;

import lombok.Data;

@Data
public class RequestConfigDto {
    private String filterName;
    private RequestConfig config;
}
