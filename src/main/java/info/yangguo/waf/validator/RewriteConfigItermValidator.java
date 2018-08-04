package info.yangguo.waf.validator;

import info.yangguo.waf.dto.RewriteConfigDto;
import info.yangguo.waf.model.RewriteType;

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
                    if (parts.length != 3)
                        return true;
                    else
                        return false;
                });
        if (match1) {
            context.buildConstraintViolationWithTemplate("Rewrite iterm pattern is illegal").addPropertyNode("iterms").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        boolean match2 = config.getIterms().parallelStream()
                .anyMatch(iterm -> {
                    String[] parts = iterm.split(" +");
                    if (RewriteType.getByCode(parts[2]).isPresent())
                        return false;
                    else
                        return true;
                });
        if (match2) {
            context.buildConstraintViolationWithTemplate("Rewrite iterm type must be last or break or redirect or permanent").addPropertyNode("iterms").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        return true;
    }
}
