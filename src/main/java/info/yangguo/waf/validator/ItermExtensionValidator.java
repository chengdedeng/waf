package info.yangguo.waf.validator;

import info.yangguo.waf.dto.RequestItermConfigDto;
import info.yangguo.waf.request.CCHttpRequestFilter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ItermExtensionValidator implements ConstraintValidator<CheckItermConfig, RequestItermConfigDto> {
    @Override
    public void initialize(CheckItermConfig constraintAnnotation) {
    }

    @Override
    public boolean isValid(RequestItermConfigDto config, ConstraintValidatorContext context) {
        if (CCHttpRequestFilter.class.getName().equals(config.getFilterName())) {
            boolean isMatch = config.getExtension().values().stream().anyMatch(value -> {
                if (value instanceof Integer)
                    return false;
                else
                    return true;

            });
            if (isMatch) {
                context.buildConstraintViolationWithTemplate("CCHttpRequestFilter extension value must be integer").addPropertyNode("extension").addBeanNode().inIterable().addConstraintViolation();
                return false;
            }

            boolean isPositive = config.getExtension().values().stream().anyMatch(value -> {
                if (((Integer) value) <= 0)
                    return true;
                else
                    return false;
            });
            if (isPositive) {
                context.buildConstraintViolationWithTemplate("CCHttpRequestFilter extension value must be positive").addPropertyNode("extension").addBeanNode().inIterable().addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}