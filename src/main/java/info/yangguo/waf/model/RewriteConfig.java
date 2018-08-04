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
public class RewriteConfig implements Serializable {
    private static final long serialVersionUID = -9064865215293339346L;
    /**
     * Upstream唯一标志
     */
    private String wafRoute;
    /**
     * 配置信息
     */
    private BasicConfig config;
}
