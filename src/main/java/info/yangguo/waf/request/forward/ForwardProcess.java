package info.yangguo.waf.request.forward;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.Map;

public interface ForwardProcess {
    HttpResponse execute(HttpRequest request, Map<String, Object> args);
}
