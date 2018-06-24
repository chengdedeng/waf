package info.yangguo.waf.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ResponseConfigValidator.class})
public @interface CheckResponseConfig {
    String message() default "config is illegal";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
