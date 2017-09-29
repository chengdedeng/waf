package info.yangguo.waf.util;


import info.yangguo.waf.request.FilterType;

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
                logger.error("{}配置文件加载失败", filterType.name(),e);
            }
        }
    }

    public static List<Pattern> getPattern(String type) {
        return confMap.get(type);
    }
}
