package info.yangguo.waf.controller;

import info.yangguo.waf.config.AdminProperties;
import info.yangguo.waf.model.Result;
import info.yangguo.waf.model.User;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping(value = "api/user/")
@Api(value = "api/user/", description = "用户相关的接口")
public class UserController {
    private static Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private AdminProperties adminProperties;

    @ApiResponses(value = {@ApiResponse(code = 200, message = "登录成功"), @ApiResponse(code = 403, message = "登录失败")})
    @ApiOperation(value = "login", notes = "用户登录")
    @ResponseBody
    @PostMapping(value = "login")
    public Result login(HttpServletRequest request, @RequestBody User user) {
        Result result = new Result();
        if (adminProperties.email.equals(user.getEmail()) && adminProperties.password.equals(user.getPassword())) {
            result.setCode(HttpStatus.OK.value());
        } else {
            result.setCode(HttpStatus.FORBIDDEN.value());
        }
        request.getSession(true);
        return result;
    }

    @ApiResponses(value = {@ApiResponse(code = 200, message = "退出成功")})
    @ApiOperation(value = "logout", notes = "退出登录")
    @ResponseBody
    @GetMapping(value = "logout")
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        Result result = new Result();
        result.setCode(HttpStatus.OK.value());
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            cookie.setMaxAge(0);
            cookie.setValue(null);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        return result;
    }
}
