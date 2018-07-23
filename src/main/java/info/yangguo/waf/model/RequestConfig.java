package info.yangguo.waf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestConfig {
    /**
     * 拦截器名称
     */
    private String filterName;
    /**
     * 配置信息
     */
    private BasicConfig config;
    /**
     * 配置项
     */
    private List<ItermConfig> itermConfigs;
}
