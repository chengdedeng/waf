package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.model.SecurityConfigItem;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * @author:杨果
 * @date:2017/5/15 下午2:33
 * <p>
 * Description:
 */
public class PostSecurity extends Security {
    private static Logger logger = LoggerFactory.getLogger(PostSecurity.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, List<SecurityConfigItem> items) {
        if (originalRequest.method().name().equals("POST")) {
            if (httpObject instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) httpObject;
                String contentBody = httpContent.content().toString(UTF_8);
                //application/x-www-form-urlencoded会对报文进行编码，所以需要解析出来再匹配。
                String contentType = originalRequest.headers().getAsString(HttpHeaderNames.CONTENT_TYPE);
                if (contentBody != null) {
                    if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType)) {
                        List<NameValuePair> args = URLEncodedUtils.parse(contentBody, UTF_8);
                        for (NameValuePair pair : args) {
                            for (SecurityConfigItem item : items) {
                                if (item.getConfig().getIsStart()) {
                                    Timer itemTimer = Constant.metrics.timer("PostHttpRequestFilter[" + item.getName() + "]");
                                    Timer.Context itemContext = itemTimer.time();
                                    try {
                                        Pattern pattern = Pattern.compile(item.getName());
                                        Matcher matcher = pattern.matcher(pair.getName().toLowerCase() + "=" + pair.getValue().toLowerCase());
                                        if (matcher.find()) {
                                            hackLog(logger, originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "Post", item.getName());
                                            return true;
                                        }
                                    } finally {
                                        itemContext.stop();
                                    }
                                }
                            }
                        }
                    } else {
                        for (SecurityConfigItem item : items) {
                            if (item.getConfig().getIsStart()) {
                                Timer itemTimer = Constant.metrics.timer("PostHttpRequestFilter[" + item.getName() + "]");
                                Timer.Context itemContext = itemTimer.time();
                                try {
                                    Pattern pattern = Pattern.compile(item.getName());
                                    Matcher matcher = pattern.matcher(contentBody.toLowerCase());
                                    if (matcher.find()) {
                                        hackLog(logger, originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "Post", item.getName());
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
        return false;
    }
}
