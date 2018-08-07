package info.yangguo.waf.validator;

import info.yangguo.waf.dto.RedirectConfigDto;
import info.yangguo.waf.dto.RewriteConfigDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RedirectConfigItermValidator implements ConstraintValidator<CheckRedirectConfigIterm, RedirectConfigDto> {
    @Override
    public void initialize(CheckRedirectConfigIterm constraintAnnotation) {
    }

    @Override
    public boolean isValid(RedirectConfigDto config, ConstraintValidatorContext context) {
        boolean match1 = config.getIterms().parallelStream()
                .anyMatch(iterm -> {
                    String[] parts = iterm.split(" +");
                    if (parts.length != 2)
                        return true;
                    else
                        return false;
                });
        if (match1) {
            context.buildConstraintViolationWithTemplate("Redirect iterm pattern is illegal").addPropertyNode("iterms").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        boolean match2 = config.getIterms().parallelStream()
                .anyMatch(iterm -> {
                    String[] parts = iterm.split(" +");
                    if ("301".equals(parts[1]) || "302".equals(parts[1]))
                        return false;
                    else
                        return true;
                });
        if (match2) {
            context.buildConstraintViolationWithTemplate("Redirect type must be 301 or 302").addPropertyNode("iterms").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        return true;
    }
}
