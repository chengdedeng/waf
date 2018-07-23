package info.yangguo.waf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerBasicConfig extends Config implements Serializable {
    private static final long serialVersionUID = 9124790711127925033L;
    /**
     * 服务器权重
     */
    private Integer weight;

    @Builder
    public ServerBasicConfig(Boolean isStart, Map<String, Object> extension, Integer weight) {
        super(isStart, extension);
        this.weight = weight;
    }
}
