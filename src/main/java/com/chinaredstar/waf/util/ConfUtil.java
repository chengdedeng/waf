package com.chinaredstar.waf.util;


import com.chinaredstar.waf.request.FilterType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author:杨果
 * @date:2017/5/11 上午9:30
 *
 * Description:
 *
 */
public class ConfUtil {
    private static Logger logger = LoggerFactory.getLogger(ConfUtil.class);
    private final static Map<String, List<Pattern>> confMap = new HashMap<>();

    static {
//        //加载url参数拦截配置
//        try (InputStream argsInput = ConfUtil.class.getClassLoader().getResourceAsStream(FilterType.ARGS.name())) {
//            List argsConfs = IOUtils.readLines(argsInput);
//            List<Pattern> argsPats = new ArrayList<>();
//            for (Object argsConf : argsConfs) {
//                Pattern pattern = Pattern.compile((String) argsConf);
//                argsPats.add(pattern);
//            }
//            confMap.put(FilterType.ARGS.name(), argsPats);
//        } catch (IOException e) {
//            logger.error("{}配置文件加载失败:{}", FilterType.ARGS.name(), e);
//        }
//        //加载url路径拦截配置
//        try (InputStream argsInput = ConfUtil.class.getClassLoader().getResourceAsStream(FilterType.URL.name())) {
//            List urlConfs = IOUtils.readLines(argsInput);
//            List<Pattern> urlPats = new ArrayList<>();
//            for (Object urlConf : urlConfs) {
//                Pattern pattern = Pattern.compile((String) urlConf);
//                urlPats.add(pattern);
//            }
//            confMap.put(FilterType.URL.name(), urlPats);
//        } catch (IOException e) {
//            logger.error("{}配置文件加载失败:{}", FilterType.URL.name(), e);
//        }
//        //加载cookie拦截配置
//        try (InputStream cookieInput = ConfUtil.class.getClassLoader().getResourceAsStream(FilterType.COOKIE.name())) {
//            List cookieConfs = IOUtils.readLines(cookieInput);
//            List<Pattern> cookiePats = new ArrayList<>();
//            for (Object cookieConf : cookieConfs) {
//                Pattern pattern = Pattern.compile((String) cookieConf);
//                cookiePats.add(pattern);
//            }
//            confMap.put(FilterType.COOKIE.name(), cookiePats);
//        } catch (IOException e) {
//            logger.error("{}配置文件加载失败:{}", FilterType.COOKIE.name(), e);
//        }

        for (FilterType filterType : FilterType.values()) {
            try (InputStream inputStream = ConfUtil.class.getClassLoader().getResourceAsStream(filterType.getFileName())) {
                List confs = IOUtils.readLines(inputStream);
                List<Pattern> patterns = new ArrayList<>();
                for (Object conf : confs) {
                    Pattern pattern = Pattern.compile((String) conf);
                    patterns.add(pattern);
                }
                confMap.put(filterType.name(), patterns);
            } catch (IOException e) {
                logger.error("{}配置文件加载失败:{}", filterType.name(), e);
            }
        }
    }

    public static List<Pattern> getPattern(String type) {
        return confMap.get(type);
    }
}
