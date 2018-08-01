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
public class SecurityConfigIterm implements Serializable {
    private static final long serialVersionUID = 3823749021458846717L;
    /**
     * 名称
     */
    private String name;
    /**
     * 配置信息
     */
    private BasicConfig config;
}
