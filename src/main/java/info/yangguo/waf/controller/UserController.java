package info.yangguo.waf.controller;

import info.yangguo.waf.config.AdminProperties;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.Result;
import info.yangguo.waf.model.User;
import info.yangguo.waf.util.JsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "api/user")
@Api(value = "api/user", description = "用户相关的接口")
public class UserController {
    @Autowired
    private AdminProperties adminProperties;

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
        String token = new BCryptPasswordEncoder().encode(user.getEmail() + user.getPassword());
        Map<String, Object> session = new HashMap<>();
        session.put("email", user.getEmail());
        session.put("loginTime", new Date().getTime());
        ContextHolder.getClusterService().setSession(token, JsonUtil.toJson(session, true), Duration.ofHours(1));
        response.setHeader("Set-Cookie", "WAFTOKEN=" + token + "; Path=/");
        return result;
    }

    @ApiOperation(value = "登出")
    @ResponseBody
    @GetMapping(value = "logout")
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("WAFTOKEN".equals(cookie.getName())) {
                    ContextHolder.getClusterService().deleteSession("WAFTOKEN");
                    cookie.setMaxAge(0);
                    cookie.setValue(null);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                    break;
                }
            }
        }
        return result;
    }
}
