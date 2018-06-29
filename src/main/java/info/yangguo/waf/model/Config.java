package info.yangguo.waf.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Config implements Serializable {
    private static final long serialVersionUID = 666558172889943990L;
    @ApiModelProperty(value = "是否开启，true启用，false关闭")
    private Boolean isStart;
}
