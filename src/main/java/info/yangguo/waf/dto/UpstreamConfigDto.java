package info.yangguo.waf.dto;

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
import javax.validation.constraints.Pattern;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpstreamConfigDto {
    @NotEmpty
    @Pattern(regexp = ".*_\\d{1,5}")
    @ApiModelProperty(value = "Upstream唯一标志,规则为Host_Port,如果是域名访问,Port为80,例如:yangguo.info_80", required = true)
    private String host;
    @NotNull(groups = Exist.class)
    @Null(groups = NotExist.class)
    @ApiModelProperty(value = "开关,true启用,false关闭")
    private Boolean isStart;
}
