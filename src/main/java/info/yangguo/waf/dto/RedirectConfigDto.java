package info.yangguo.waf.dto;

import info.yangguo.waf.validator.CheckRedirectConfigItem;
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
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@CheckRedirectConfigItem(groups = Exist.class)
public class RedirectConfigDto {
    @NotEmpty
    @ApiModelProperty(value = "x-waf-route，路由标志。", required = true)
    private String wafRoute;
    @NotNull(groups = Exist.class)
    @Null(groups = NotExist.class)
    @ApiModelProperty(value = "开关，true启用，false关闭。")
    private Boolean isStart;
    @NotNull(groups = Exist.class)
    @Null(groups = NotExist.class)
    private List<String> items;
}
