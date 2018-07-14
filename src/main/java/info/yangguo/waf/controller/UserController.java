package info.yangguo.waf.controller;

import info.yangguo.waf.config.AdminProperties;
import info.yangguo.waf.dto.ResultDto;
import info.yangguo.waf.dto.UserDto;
import info.yangguo.waf.service.JwtTokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "api/user")
@Api(value = "api/user", description = "用户相关的接口")
public class UserController {
    @Autowired
    private AdminProperties adminProperties;
    @Autowired
    private JwtTokenService jwtTokenService;

    @ApiOperation(value = "登录")
    @ResponseBody
    @PostMapping(value = "login")
    public ResultDto login(HttpServletResponse response, @RequestBody @Validated UserDto userDto) {
        ResultDto resultDto = new ResultDto();
        if (adminProperties.email.equals(userDto.getEmail()) && adminProperties.password.equals(userDto.getPassword())) {
            resultDto.setCode(HttpStatus.OK.value());
        } else {
            resultDto.setCode(HttpStatus.FORBIDDEN.value());
        }
        Map<String, String> claims = new HashMap<>();
        claims.put("email", userDto.getEmail());
        String token = jwtTokenService.genToken(claims);
        response.setHeader("Set-Cookie", "WAFTOKEN=" + token + "; Path=/");
        return resultDto;
    }
}
