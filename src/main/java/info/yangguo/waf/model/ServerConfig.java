package info.yangguo.waf.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerConfig extends Config implements Serializable {
    private static final long serialVersionUID = 9124790711127925033L;
    @ApiModelProperty("节点权重")
    private Integer weight;
}
