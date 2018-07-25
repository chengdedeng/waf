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
public class ResponseConfig implements Serializable {
    private static final long serialVersionUID = 2031500986318114814L;
    /**
     * 拦截器名称
     */
    private String filterName;
    /**
     * 配置信息
     */
    private BasicConfig config;
}
