package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.model.SecurityConfigItem;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * 目前这个比较粗糙，只是对multipart/form-data上传的文件名进行了检测。
 * 文件类型应该去读文件头
 * 二进制流上传目前也没有处理
 */
public class FileSecurity extends Security {
    private static Logger logger = LoggerFactory.getLogger(FileSecurity.class);
    private static Pattern filePattern = Pattern.compile("Content-Disposition: form-data;(.+)filename=\"(.+)\\.(.*)\"");

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, List<SecurityConfigItem> items) {
        if (originalRequest.method().name().equals("POST")) {
            if (httpObject instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) httpObject;
                String contentType = originalRequest.headers().getAsString(HttpHeaderNames.CONTENT_TYPE);
                if (ContentType.MULTIPART_FORM_DATA.getMimeType().equals(contentType)) {
                    String contentBody = httpContent.content().toString(UTF_8);
                    if (contentBody != null) {
                        Matcher fileMatcher = filePattern.matcher(contentBody);
                        if (fileMatcher.find()) {
                            String fileExt = fileMatcher.group(3);
                            for (SecurityConfigItem item : items) {
                                if (item.getConfig().getIsStart()) {
                                    Timer itemTimer = Constant.metrics.timer("FileSecurity[" + item.getName() + "]");
                                    Timer.Context itemContext = itemTimer.time();
                                    try {
                                        Pattern pattern = Pattern.compile(item.getName());
                                        if (pattern.matcher(fileExt).matches()) {
                                            hackLog(logger, originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "File", item.getName());
                                            return true;
                                        }
                                    } finally {
                                        itemContext.stop();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
