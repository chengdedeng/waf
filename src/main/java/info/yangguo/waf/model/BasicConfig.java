package info.yangguo.waf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicConfig extends Config implements Serializable {
    private static final long serialVersionUID = 666558172889943990L;

    @Builder
    public BasicConfig(Boolean isStart, Map<String, Object> extension) {
        super(isStart, extension);
    }
}
