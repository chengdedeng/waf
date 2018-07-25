package info.yangguo.waf.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Config implements Serializable {
    private static final long serialVersionUID = 5343330681233227162L;
    /**
     * 开关,ture开启,false关闭
     */
    private Boolean isStart;
    /**
     * 自定义扩展信息
     */
    private Map<String, Object> extension;
}
