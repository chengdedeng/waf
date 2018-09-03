package info.yangguo.waf.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {RedirectConfigItemValidator.class})
public @interface CheckRedirectConfigItem {
    String message() default "item is illegal";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
