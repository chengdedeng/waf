package info.yangguo.waf.config;

import com.google.common.collect.Maps;
import info.yangguo.waf.dto.ResultDto;
import info.yangguo.waf.exception.UnauthorizedException;
import info.yangguo.waf.util.JsonUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@ControllerAdvice
@ResponseBody
public class ExceptionHandlerAdvice {
    private final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultDto handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.warn(ExceptionUtils.getFullStackTrace(e));
        addCorsHeader(request, response);
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.BAD_REQUEST.value());
        resultDto.setValue("数据格式错误");
        return resultDto;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultDto handleArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.warn(ExceptionUtils.getFullStackTrace(e));
        addCorsHeader(request, response);
        List<ObjectError> errorList = e.getBindingResult().getAllErrors();
        Map errMap = Maps.newHashMap();
        for (ObjectError err : errorList) {
            if (err instanceof FieldError)
                errMap.put(((FieldError) err).getField(), err.getDefaultMessage());
            else if (err instanceof ObjectError)
                errMap.put(((ObjectError) err).getObjectName(), err.getDefaultMessage());
        }
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.BAD_REQUEST.value());
        resultDto.setValue(JsonUtil.toJson(errMap, true));

        return resultDto;
    }


    @ExceptionHandler(ServletRequestBindingException.class)
    public ResultDto handleServletRequestBindingException(ServletRequestBindingException e, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.error(ExceptionUtils.getFullStackTrace(e));
        addCorsHeader(request, response);
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.BAD_REQUEST.value());
        resultDto.setValue("Header/Body不正确");
        return resultDto;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResultDto handleServletRequestUnauthorizedException(UnauthorizedException e, HttpServletRequest request, HttpServletResponse response) {
        addCorsHeader(request, response);
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.UNAUTHORIZED.value());
        resultDto.setValue("请登录");
        return resultDto;
    }

    private void addCorsHeader(HttpServletRequest request, HttpServletResponse response) {
        if (CorsUtils.isCorsRequest(request)) {
            Enumeration<String> headers = request.getHeaderNames();
            String origin = null;
            while (headers.hasMoreElements()) {
                String header = headers.nextElement();
                if (header.toLowerCase().equals("origin")) {
                    origin = request.getHeader(header);
                }
            }
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
    }
}
