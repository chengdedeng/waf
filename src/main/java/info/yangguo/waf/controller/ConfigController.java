package info.yangguo.waf.controller;

import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.*;
import io.atomix.utils.time.Versioned;
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
    @GetMapping(value = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result getConfig(@RequestParam(required = false) String filterName) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        if (filterName == null) {
            result.setValue(ContextHolder.getConfigs().asJavaMap().keySet());
        } else {
            result.setValue(ContextHolder.getConfigs().asJavaMap().get(filterName));
        }
        return result;
    }

    @ApiOperation(value = "设置RequestFilter")
    @ResponseBody
    @PostMapping(value = "request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result setRequestConfig(@RequestBody RequestConfigDto requestConfigDto) {
        Versioned<Config> versioned = ContextHolder.getConfigs().get(requestConfigDto.getFilterName());
        RequestConfig allConfig = (RequestConfig) versioned.value();
        allConfig.setIsStart(requestConfigDto.getConfig().getIsStart());
        requestConfigDto.getConfig().getRules().stream().forEach(rule -> {
            allConfig.getRules().remove(rule);
            allConfig.getRules().add(rule);
        });
        ContextHolder.getConfigs().replace(requestConfigDto.getFilterName(), versioned.version(), allConfig);
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
    public Result delRequestConfig(@RequestBody RequestConfigDto requestConfigDto) {
        Versioned<Config> versioned = ContextHolder.getConfigs().get(requestConfigDto.getFilterName());
        RequestConfig allConfig = (RequestConfig) versioned.value();
        requestConfigDto.getConfig().getRules().stream().forEach(rule -> {
            allConfig.getRules().remove(rule);
        });
        ContextHolder.getConfigs().replace(requestConfigDto.getFilterName(), versioned.version(), allConfig);
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        return result;
    }

    @ApiOperation(value = "设置ResponseFilter")
    @ResponseBody
    @PostMapping(value = "response")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    required = false, dataType = "string", paramType = "cookie")
    })
    public Result setResponseConfig(@RequestBody ResponseConfigDto responseResponseConfigDto) {
        Versioned<Config> versioned = ContextHolder.getConfigs().get(responseResponseConfigDto.getFilterName());
        Config allConfig = versioned.value();
        allConfig.setIsStart(responseResponseConfigDto.getConfig().getIsStart());
        ContextHolder.getConfigs().replace(responseResponseConfigDto.getFilterName(), versioned.version(), allConfig);
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        return result;
    }
}
