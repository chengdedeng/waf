package info.yangguo.waf.controller;

import info.yangguo.waf.model.Result;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping(value = "api/config/")
@Api(value = "api/config/", description = "配置相关的接口")
public class ConfigController {
    @ApiResponses(value = {@ApiResponse(code = 200, message = "请求成功")})
    @ApiOperation(value = "info", notes = "退出登录")
    @ResponseBody
    @GetMapping(value = "info")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "JSESSIONID", value = "JSESSIONID",
                    required = true, dataType = "string", paramType = "cookie")
    })
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
