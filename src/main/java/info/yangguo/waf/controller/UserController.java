package info.yangguo.waf.controller;

import info.yangguo.waf.config.AdminProperties;
import info.yangguo.waf.model.Result;
import info.yangguo.waf.model.User;
import info.yangguo.waf.service.JwtTokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public Result login(HttpServletResponse response, @RequestBody User user) {
        Result result = new Result();
        if (adminProperties.email.equals(user.getEmail()) && adminProperties.password.equals(user.getPassword())) {
            result.setCode(HttpStatus.OK.value());
        } else {
            result.setCode(HttpStatus.FORBIDDEN.value());
        }
        Map<String, String> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        String token = jwtTokenService.genToken(claims);
        response.setHeader("Set-Cookie", "WAFTOKEN=" + token + "; Path=/");
        return result;
    }
}
