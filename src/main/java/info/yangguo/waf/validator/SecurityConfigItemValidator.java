package info.yangguo.waf.validator;

import info.yangguo.waf.dto.SecurityConfigItemDto;
import info.yangguo.waf.request.security.CCSecurity;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SecurityConfigItemValidator implements ConstraintValidator<CheckSecurityConfigItem, SecurityConfigItemDto> {
    @Override
    public void initialize(CheckSecurityConfigItem constraintAnnotation) {
    }

    @Override
    public boolean isValid(SecurityConfigItemDto config, ConstraintValidatorContext context) {
        if (CCSecurity.class.getName().equals(config.getFilterName())) {
            if (config.getExtension() == null) {
                context.buildConstraintViolationWithTemplate("CCSecurity extension value can't be null").addPropertyNode("extension").addBeanNode().inIterable().addConstraintViolation();
                return false;
            }
            boolean isMatch = config.getExtension().values().stream().anyMatch(value -> {
                if (value instanceof Integer)
                    return false;
                else
                    return true;

            });
            if (isMatch) {
                context.buildConstraintViolationWithTemplate("CCSecurity extension value must be integer").addPropertyNode("extension").addBeanNode().inIterable().addConstraintViolation();
                return false;
            }

            boolean isPositive = config.getExtension().values().stream().anyMatch(value -> {
                if (((Integer) value) <= 0)
                    return true;
                else
                    return false;
            });
            if (isPositive) {
                context.buildConstraintViolationWithTemplate("CCSecurity extension value must be positive").addPropertyNode("extension").addBeanNode().inIterable().addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}