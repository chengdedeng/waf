package info.yangguo.waf.controller;

import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.Result;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "api/config/")
@Api(value = "api/config/", description = "配置相关的接口")
public class ConfigController {

    @ApiResponses(value = {@ApiResponse(code = 200, message = "请求成功")})
    @ApiOperation(value = "info", notes = "获取配置")
    @ResponseBody
    @GetMapping(value = "info")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result getInfo() {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        result.setValue(ContextHolder.getConfigs().asJavaMap());
        return result;
    }
}
