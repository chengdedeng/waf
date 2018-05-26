package info.yangguo.waf.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

@Data
public class RequestConfig extends Config implements Serializable {
    private static final long serialVersionUID = -2092674800835150369L;
    private Set<Rule> rules;


    @Getter
    @Setter
    public static class Rule extends Config implements Serializable {
        private static final long serialVersionUID = 3823749021458846717L;
        private String regex;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Rule rule = (Rule) o;
            return Objects.equals(regex, rule.regex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regex);
        }
    }
}
