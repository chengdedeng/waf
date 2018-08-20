package info.yangguo.waf.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import info.yangguo.waf.request.forward.ForwardProcess;
import info.yangguo.waf.request.forward.http.Swagger2;
import info.yangguo.waf.util.JsonUtil;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;

import java.util.List;
import java.util.Map;

import static io.netty.util.CharsetUtil.UTF_8;

public class ForwardFilter implements RequestFilter {
    public Map<String, List<ForwardProcess>> processes = Maps.newHashMap();

    public ForwardFilter() {
        List<ForwardProcess> httpForward = Lists.newArrayList();
        httpForward.add(new Swagger2());
    }

    @Override
    public HttpResponse doFilter(HttpRequest originalRequest, HttpObject httpObject) {
        if (httpObject instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) httpObject;
            if (request.decoderResult() == DecoderResult.SUCCESS
                    && HttpHeaderValues.APPLICATION_JSON.toString().equals(originalRequest.headers().get(HttpHeaderNames.CONTENT_TYPE))) {
                Map<String, Object> parameters = null;
                try {
                    String contentBody = request.content().toString(UTF_8);
                    parameters = (Map<String, Object>) JsonUtil.fromJson(contentBody, Map.class);
                } finally {
                    request.content().release();
                }

            }
        }
        return null;
    }
}
