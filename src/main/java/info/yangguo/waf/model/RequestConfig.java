package info.yangguo.waf.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestConfig extends Config implements Serializable {
    private static final long serialVersionUID = -2092674800835150369L;
    @ApiModelProperty(value = "规则")
    private Set<Rule> rules;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Rule extends Config implements Serializable {
        private static final long serialVersionUID = 3823749021458846717L;
        @ApiModelProperty(value = "正则表达式", example = "^/a/b/c$", required = true)
        private String regex;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Rule rule = (Rule) o;
            return Objects.equals(regex, rule.regex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), regex);
        }
    }
}
