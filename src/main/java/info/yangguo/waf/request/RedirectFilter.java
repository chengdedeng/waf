package info.yangguo.waf.request;

import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.BasicConfig;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.CharEncoding.UTF_8;

public class RedirectFilter {
    private static Logger logger = LoggerFactory.getLogger(RedirectFilter.class);

    public static HttpResponse doFilter(HttpRequest originalRequest, HttpObject httpObject) {
        HttpResponse httpResponse = null;
        if (httpObject instanceof HttpRequest) {
            String wafRoute = originalRequest.headers().getAsString(WafHttpHeaderNames.X_WAF_ROUTE);
            String redirectUri = originalRequest.uri();
            try {
                redirectUri = UriUtils.decode(originalRequest.uri(), UTF_8);
            } catch (Exception e) {
                logger.warn("uri decode is failed.", e);
            }

            Map<String, BasicConfig> redirectConfig = ContextHolder.getClusterService().getRedirectConfigs();
            if (redirectConfig.containsKey(wafRoute)) {
                for (Map.Entry<String, Object> entry : redirectConfig.get(wafRoute).getExtension().entrySet()) {
                    Pattern redirectPattern = Pattern.compile(entry.getKey());
                    Matcher redirectMatcher = redirectPattern.matcher(redirectUri);
                    if (redirectMatcher.matches()) {
                        String[] parts = ((String) entry.getValue()).split(" +");
                        httpResponse = createResponse(HttpResponseStatus.valueOf(Integer.valueOf(parts[1])), originalRequest, parts[0]);
                    }
                }
            }
        }
        return httpResponse;
    }


    private static HttpResponse createResponse(HttpResponseStatus httpResponseStatus, HttpRequest originalRequest, String host) {
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        httpHeaders.add(HttpHeaderNames.CONNECTION, "close");
        httpHeaders.add(HttpHeaderNames.LOCATION, host + originalRequest.uri());
        //I/O error while reading input message; nested exception is java.net.SocketTimeoutException
        HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus);

        //support CORS
        String origin = originalRequest.headers().getAsString(HttpHeaderNames.ORIGIN);
        if (origin != null) {
            httpHeaders.set("Access-Control-Allow-Credentials", "true");
            httpHeaders.set("Access-Control-Allow-Origin", origin);
        }
        httpResponse.headers().add(httpHeaders);
        return httpResponse;
    }
}

