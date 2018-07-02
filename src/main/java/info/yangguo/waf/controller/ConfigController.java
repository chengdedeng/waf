package info.yangguo.waf.controller;

import com.google.common.collect.Lists;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "api/config")
@Api(value = "api/config", description = "配置相关的接口")
public class ConfigController {

    @ApiOperation(value = "获取Filter配置")
    @ResponseBody
    @GetMapping(value = "request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public Result<List<RequestConfigDto>> getRequestConfigs() {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        List<RequestConfigDto> value = Lists.newArrayList();
        ContextHolder.getClusterService().getRequestConfigs().entrySet().stream().forEach(entry -> {
            RequestConfigDto requestConfigDto = RequestConfigDto.builder().filterName(entry.getKey()).config(entry.getValue()).build();
            value.add(requestConfigDto);
        });
        result.setValue(value);
        return result;
    }

    @ApiOperation(value = "获取Filter配置")
    @ResponseBody
    @GetMapping(value = "response")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public Result<List<ResponseConfigDto>> getResponseConfigs() {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        List<ResponseConfigDto> value = Lists.newArrayList();
        ContextHolder.getClusterService().getResponseConfigs().entrySet().stream().forEach(entry -> {
            ResponseConfigDto responseConfigDto = ResponseConfigDto.builder().filterName(entry.getKey()).config(entry.getValue()).build();
            value.add(responseConfigDto);
        });
        result.setValue(value);
        return result;
    }


    @ApiOperation(value = "设置RequestFilter")
    @ResponseBody
    @PutMapping(value = "request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public Result setRequestConfig(@RequestBody @Validated RequestConfigDto requestConfigDto) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        RequestConfig config = requestConfigDto.getConfig();
        if (config.getIsStart() != null)
            ContextHolder.getClusterService().setRequestSwitch(requestConfigDto.getFilterName(), config.getIsStart());
        if (config.getRules() != null)
            config.getRules().stream().forEach(rule -> {
                ContextHolder.getClusterService().setRequestRule(requestConfigDto.getFilterName(), rule.getRegex(), rule.getIsStart());
            });
        return result;
    }

    @ApiOperation(value = "删除RequestFilter中Rule")
    @ResponseBody
    @DeleteMapping(value = "request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public Result deleteRequestConfig(@RequestBody @Validated RequestConfigDto requestConfigDto) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());

        requestConfigDto.getConfig().getRules().stream().forEach(rule -> {
            ContextHolder.getClusterService().deleteRequestRule(requestConfigDto.getFilterName(), rule.getRegex());
        });

        return result;
    }

    @ApiOperation(value = "设置ResponseFilter")
    @ResponseBody
    @PutMapping(value = "response")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public Result setResponseConfig(@RequestBody @Validated ResponseConfigDto responseConfigDto) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().setResponseSwitch(responseConfigDto.getFilterName(), responseConfigDto.getConfig().getIsStart());
        return result;
    }

    @ApiOperation(value = "获取Upstream配置")
    @ResponseBody
    @GetMapping(value = "upstream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public Result<List<UpstreamDto>> getUpstreamConfig() {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        List<UpstreamDto> upstreamDtos = ContextHolder.getClusterService().getUpstreamConfig().entrySet().stream().map(entry1 -> {
            UpstreamDto upstreamDto = UpstreamDto.builder().host(entry1.getKey()).isStart(entry1.getValue().getIsStart()).build();

            Map<String, UpstreamDto.ServerDto> map = entry1.getValue().getServersMap().entrySet().stream().collect(Collectors.toMap(entry2 -> {
                return entry2.getKey();
            }, entry2 -> {
                return UpstreamDto.ServerDto.builder()
                        .ip(entry2.getValue().getIp())
                        .port(entry2.getValue().getPort())
                        .isStart(entry2.getValue().getServerConfig().getIsStart())
                        .weight(entry2.getValue().getServerConfig().getWeight())
                        .build();
            }));

            entry1.getValue().getUnhealthilyServers().stream().forEach(server -> {
                map.get(server.getIp() + "_" + server.getPort()).setIsHealth(false);
            });
            entry1.getValue().getHealthilyServers().stream().forEach(server -> {
                map.get(server.getIp() + "_" + server.getPort()).setIsHealth(true);
            });

            List<UpstreamDto.ServerDto> serverDtos = map.entrySet().stream().map(entry3 -> {
                return entry3.getValue();
            }).collect(Collectors.toList());

            upstreamDto.setServers(serverDtos);

            return upstreamDto;
        }).collect(Collectors.toList());
        result.setValue(upstreamDtos);
        return result;
    }

    @ApiOperation(value = "设置Upstream")
    @ResponseBody
    @PutMapping(value = "upstream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public Result setUpstreamConfig(@RequestBody @Validated UpstreamDto upstreamDto) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        if (upstreamDto.getIsStart() != null) {
            ContextHolder.getClusterService().setUpstream(Optional.of(upstreamDto.getHost()), Optional.of(upstreamDto.getIsStart()));
        }
        if (upstreamDto.getServers() != null) {
            upstreamDto.getServers().stream().forEach(serverDto -> {
                ContextHolder.getClusterService().setUpstreamServer(Optional.of(upstreamDto.getHost()),
                        Optional.of(serverDto.getIp()),
                        Optional.of(serverDto.getPort()),
                        Optional.ofNullable(serverDto.getIsStart()),
                        Optional.ofNullable(serverDto.getWeight()));
            });
        }

        return result;
    }
}
