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
package info.yangguo.waf.dto;

import info.yangguo.waf.model.ForwardType;
import info.yangguo.waf.validator.Exist;
import info.yangguo.waf.validator.NotExist;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForwardConfigItemDto {
    @NotEmpty
    @ApiModelProperty(value = "x-waf-route，路由标志。", required = true)
    private String wafRoute;
    @NotEmpty
    @ApiModelProperty(value = "配置项名称。", required = true)
    private String name;
    @NotNull(groups = Exist.class)
    @Null(groups = NotExist.class)
    @ApiModelProperty(value = "是否开启，true启用，false关闭。")
    private Boolean isStart;
    @Null(groups = NotExist.class)
    @ApiModelProperty(value = "item扩展信息，目前只在CCSecurityFilter有使用。")
    private ForwardType type;
}
