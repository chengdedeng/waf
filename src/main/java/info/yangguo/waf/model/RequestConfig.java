package info.yangguo.waf.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
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
    }
}
