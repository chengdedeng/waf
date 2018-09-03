package info.yangguo.waf.dto;

import info.yangguo.waf.model.ForwardType;
import info.yangguo.waf.validator.Exist;
import info.yangguo.waf.validator.NotExist;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForwardConfigItemDto {
    @NotEmpty
    @ApiModelProperty(value = "x-waf-route，路由标志。", required = true)
    private String wafRoute;
    @NotEmpty
    @ApiModelProperty(value = "配置项名称。", required = true)
    private String name;
    @NotNull(groups = Exist.class)
    @Null(groups = NotExist.class)
    @ApiModelProperty(value = "是否开启，true启用，false关闭。")
    private Boolean isStart;
    @Null(groups = NotExist.class)
    @ApiModelProperty(value = "item扩展信息，目前只在CCSecurityFilter有使用。")
    private ForwardType type;
}
