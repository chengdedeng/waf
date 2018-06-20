package info.yangguo.waf.controller;

import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.ConfigType;
import info.yangguo.waf.model.RequestConfigDto;
import info.yangguo.waf.model.ResponseConfigDto;
import info.yangguo.waf.model.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "api/config")
@Api(value = "api/config", description = "配置相关的接口")
public class ConfigController {

    @ApiOperation(value = "获取配置")
    @ResponseBody
    @GetMapping(value = "{type}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result getConfigs(@PathVariable(value = "type") ConfigType type) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        if (ConfigType.request.equals(type)) {
            result.setValue(ContextHolder.getClusterService().getRequestConfigs());
        } else if (ConfigType.response.equals(type)) {
            result.setValue(ContextHolder.getClusterService().getResponseConfigs());
        } else {
            result.setCode(HttpStatus.BAD_REQUEST.value());
        }
        return result;
    }


    @ApiOperation(value = "设置RequestFilter")
    @ResponseBody
    @PutMapping(value = "request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result setRequestConfig(@RequestBody RequestConfigDto requestConfigDto) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        return result;
    }

    @ApiOperation(value = "删除RequestFilter中Rule")
    @ResponseBody
    @DeleteMapping(value = "request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result deleteRequestConfig(@RequestBody RequestConfigDto requestConfigDto) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        return result;
    }

    @ApiOperation(value = "设置ResponseFilter")
    @ResponseBody
    @PutMapping(value = "response")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result setResponseConfig(@RequestBody ResponseConfigDto responseConfigDto) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        return result;
    }
}
