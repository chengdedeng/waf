package info.yangguo.waf.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Config implements Serializable {
    private static final long serialVersionUID = 666558172889943990L;
    private Boolean isStart;
}
