/*
 * Copyright 2018-present yangguo@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
