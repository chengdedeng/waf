package info.yangguo.waf.validator;

import info.yangguo.waf.dto.RedirectConfigDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RedirectConfigItemValidator implements ConstraintValidator<CheckRedirectConfigItem, RedirectConfigDto> {
    @Override
    public void initialize(CheckRedirectConfigItem constraintAnnotation) {
    }

    @Override
    public boolean isValid(RedirectConfigDto config, ConstraintValidatorContext context) {
        boolean match1 = config.getItems().parallelStream()
                .anyMatch(item -> {
                    String[] parts = item.split(" +");
                    if (parts.length != 3)
                        return true;
                    else
                        return false;
                });
        if (match1) {
            context.buildConstraintViolationWithTemplate("Redirect item pattern is illegal").addPropertyNode("items").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        boolean match2 = config.getItems().parallelStream()
                .anyMatch(item -> {
                    String[] parts = item.split(" +");
                    if (parts[1].startsWith("http://") || parts[1].startsWith("https://"))
                        return false;
                    else
                        return true;
                });
        if (match2) {
            context.buildConstraintViolationWithTemplate("Redirect host must be http or https").addPropertyNode("items").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        boolean match3 = config.getItems().parallelStream()
                .anyMatch(item -> {
                    String[] parts = item.split(" +");
                    if ("301".equals(parts[2]) || "302".equals(parts[2]))
                        return false;
                    else
                        return true;
                });
        if (match3) {
            context.buildConstraintViolationWithTemplate("Redirect type must be 301 or 302").addPropertyNode("items").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        return true;
    }
}
