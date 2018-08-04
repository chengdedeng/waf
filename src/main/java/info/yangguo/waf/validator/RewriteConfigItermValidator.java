package info.yangguo.waf.validator;

import info.yangguo.waf.dto.RewriteConfigDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RewriteConfigItermValidator implements ConstraintValidator<CheckRewriteConfigIterm, RewriteConfigDto> {
    @Override
    public void initialize(CheckRewriteConfigIterm constraintAnnotation) {
    }

    @Override
    public boolean isValid(RewriteConfigDto config, ConstraintValidatorContext context) {
        boolean match1 = config.getIterms().parallelStream()
                .anyMatch(iterm -> {
                    String[] parts = iterm.split(" +");
                    if (parts.length != 2)
                        return true;
                    else
                        return false;
                });
        if (match1) {
            context.buildConstraintViolationWithTemplate("Rewrite iterm pattern is illegal").addPropertyNode("iterms").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        return true;
    }
}
