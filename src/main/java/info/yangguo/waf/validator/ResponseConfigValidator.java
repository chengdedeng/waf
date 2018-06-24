package info.yangguo.waf.validator;

import info.yangguo.waf.model.Config;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ResponseConfigValidator implements ConstraintValidator<CheckResponseConfig, Config> {
    @Override
    public void initialize(CheckResponseConfig constraintAnnotation) {
    }

    @Override
    public boolean isValid(Config value, ConstraintValidatorContext context) {
        context.buildConstraintViolationWithTemplate("isStart of config is illegal").addPropertyNode("isStart").addConstraintViolation();
        return value.getIsStart() != null;
    }
}
