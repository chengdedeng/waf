package info.yangguo.waf.validator;

import info.yangguo.waf.dto.RewriteConfigDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RewriteConfigItemValidator implements ConstraintValidator<CheckRewriteConfigItem, RewriteConfigDto> {
    @Override
    public void initialize(CheckRewriteConfigItem constraintAnnotation) {
    }

    @Override
    public boolean isValid(RewriteConfigDto config, ConstraintValidatorContext context) {
        boolean match1 = config.getItems().parallelStream()
                .anyMatch(item -> {
                    String[] parts = item.split(" +");
                    if (parts.length != 2)
                        return true;
                    else
                        return false;
                });
        if (match1) {
            context.buildConstraintViolationWithTemplate("Rewrite item pattern is illegal").addPropertyNode("items").addBeanNode().inIterable().addConstraintViolation();
            return false;
        }

        return true;
    }
}
