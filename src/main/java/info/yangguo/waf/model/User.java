package info.yangguo.waf.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.NotNull;

@Data
public class User {
    @NotNull
    @Email
    @ApiModelProperty(value = "邮箱", required = true)
    private String email;
    @NotNull
    @ApiModelProperty(value = "密码", required = true)
    private String password;
}
