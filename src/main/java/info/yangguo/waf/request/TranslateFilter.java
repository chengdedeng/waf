package info.yangguo.waf.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.ForwardConfig;
import info.yangguo.waf.model.ForwardConfigIterm;
import info.yangguo.waf.model.ForwardType;
import info.yangguo.waf.request.translate.TranslateProcess;
import info.yangguo.waf.request.translate.http.Swagger2;
import info.yangguo.waf.util.JsonUtil;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.util.CharsetUtil.UTF_8;

public class TranslateFilter implements RequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslateFilter.class);
    public Map<ForwardType, List<TranslateProcess>> processes = Maps.newHashMap();

    public TranslateFilter() {
        List<TranslateProcess> httpForward = Lists.newArrayList();
        httpForward.add(new Swagger2());
        processes.put(ForwardType.HTTP, httpForward);
    }

    @Override
    public HttpResponse doFilter(HttpRequest originalRequest, HttpObject httpObject) {
        String wafRoute = originalRequest.headers().getAsString(WafHttpHeaderNames.X_WAF_ROUTE);
        HttpResponse response = null;
        //首先得要waf route匹配上
        if (ContextHolder.getClusterService().getTranslateConfigs().containsKey(wafRoute)) {
            //其次必须要是full http request
            if (httpObject instanceof FullHttpRequest) {
                FullHttpRequest request = (FullHttpRequest) httpObject;
                //最后request decode必须要成功且content type要为json
                if (request.decoderResult() == DecoderResult.SUCCESS
                        && HttpHeaderValues.APPLICATION_JSON.toString().equals(originalRequest.headers().getAsString(HttpHeaderNames.CONTENT_TYPE))) {
                    Map<String, Object> parameters = null;
                    String contentBody = request.content().toString(UTF_8);
                    if (StringUtils.isNotBlank(contentBody))
                        parameters = (Map<String, Object>) JsonUtil.fromJson(contentBody, Map.class);
                    String uri = originalRequest.uri();
                    try {
                        uri = UriUtils.decode(originalRequest.uri(), CharEncoding.UTF_8);
                    } catch (Exception e) {
                        LOGGER.warn("uri decode is failed.", e);
                    }

                    Optional<ForwardConfig> translateConfig = Optional.ofNullable(ContextHolder.getClusterService().getTranslateConfigs().get(wafRoute));
                    String finalUri = uri;

                    if (translateConfig.isPresent() && translateConfig.get().getConfig().getIsStart()) {
                        Optional<ForwardType> forwardType = Optional.ofNullable(null);
                        for (ForwardConfigIterm iterm : translateConfig.get().getForwardConfigIterms()) {
                            if (iterm.getConfig().getIsStart()) {
                                Pattern pattern = Pattern.compile(iterm.getName());
                                Matcher matcher = pattern.matcher(finalUri);
                                if (matcher.matches()) {
                                    //type这个参数有点魔法，因为我们在配置的扩展信息里面就用type作为key表示forward的类型
                                    forwardType = Optional.ofNullable(ForwardType.getType(String.valueOf(iterm.getConfig().getExtension().get("type"))));
                                    break;
                                }
                            }
                        }
                        if (forwardType.isPresent()) {
                            for (TranslateProcess process : processes.get(forwardType.get())) {
                                Pattern pattern = Pattern.compile(process.getWafRoutePattern());
                                Matcher matcher = pattern.matcher(wafRoute);
                                if (matcher.matches()) {
                                    response = process.execute(wafRoute, uri, parameters);
                                    if (response != null)
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return response;
    }
}
