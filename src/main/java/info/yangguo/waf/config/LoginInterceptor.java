package info.yangguo.waf.config;

import info.yangguo.waf.exception.UnauthorizedException;
import info.yangguo.waf.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object object) throws UnauthorizedException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("WAFTOKEN".equals(cookie.getName())) {
                    if (jwtTokenService.verifyToken(cookie.getValue())) {
                        return true;
                    } else {
                        throw new UnauthorizedException();
                    }
                }
            }
        }
        throw new UnauthorizedException();
    }
}
