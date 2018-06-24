package info.yangguo.waf.validator;

import info.yangguo.waf.model.RequestConfig;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RequestConfigValidator implements ConstraintValidator<CheckRequestConfig, RequestConfig> {
    @Override
    public void initialize(CheckRequestConfig constraintAnnotation) {
    }

    @Override
    public boolean isValid(RequestConfig value, ConstraintValidatorContext context) {
        if (value.getRules() == null)
            return true;
        else {
            boolean isFind = value.getRules().stream().anyMatch(rule -> {
                if (rule.getRegex() == null)
                    return true;
                else
                    return false;
            });
            context.buildConstraintViolationWithTemplate("regex of rule is illegal").addPropertyNode("rules").addBeanNode().inIterable().addConstraintViolation();
            return !isFind;
        }
    }
}
