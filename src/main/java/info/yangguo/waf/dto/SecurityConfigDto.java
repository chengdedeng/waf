package info.yangguo.waf.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SecurityConfigDto {
    @NotEmpty
    @Pattern(regexp = "info\\.yangguo\\.waf\\.request\\.security\\..*Security")
    @ApiModelProperty(value = "security拦截器名称。", required = true)
    private String filterName;
    @NotNull
    @ApiModelProperty(value = "是否开启，true启用，false关闭。")
    private Boolean isStart;
}
