package info.yangguo.waf.request.forward;

import io.netty.handler.codec.http.HttpResponse;

import java.util.Map;

public interface ForwardProcess {
    /**
     * 需要被处理的waf route正则表达式，匹配则进入该处理器进行处理，真正的处理逻辑
     * 在execute中。
     *
     * @return
     */
    String getWafRoutePattern();

    /**
     * forward处理器
     *
     * @param wafRoute
     * @param uri
     * @param args
     * @return
     */
    HttpResponse execute(String wafRoute, String uri, Map<String, Object> args);
}
