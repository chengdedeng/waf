package info.yangguo.waf.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForwardConfigDto {
    @NotEmpty
    @ApiModelProperty(value = "x-waf-route，路由标志。", required = true)
    private String wafRoute;
    @NotNull
    @ApiModelProperty(value = "是否开启，true启用，false关闭。")
    private Boolean isStart;
}
