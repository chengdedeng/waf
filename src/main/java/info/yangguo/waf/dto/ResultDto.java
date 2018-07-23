package info.yangguo.waf.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("响应结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultDto<T> {
    @ApiModelProperty(value = "结果代码")
    private int code;
    @ApiModelProperty(value = "结果对象")
    private T value;
}
