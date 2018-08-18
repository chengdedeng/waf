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
public class ForwardConfig implements Serializable {
    private static final long serialVersionUID = -2551198849518304264L;
    /**
     * 路由标志
     */
    private String wafRoute;
    /**
     * 配置信息
     */
    private BasicConfig config;
    /**
     * 配置项
     */
    private List<ForwardConfigIterm> forwardConfigIterms;
}
