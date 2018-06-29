package info.yangguo.waf.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @NotNull
    @Email
    @ApiModelProperty(value = "邮箱", required = true)
    private String email;
    @NotNull
    @ApiModelProperty(value = "密码", required = true)
    private String password;
}
