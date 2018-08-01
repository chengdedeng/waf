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
public class SecurityConfig implements Serializable {
    private static final long serialVersionUID = 7698988201535399962L;
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
    private List<SecurityConfigIterm> securityConfigIterms;
}
