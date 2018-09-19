package info.yangguo.waf.request.translate.dubbo;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import info.yangguo.waf.request.translate.TranslateProcess;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public enum HelloServiceTranslate implements TranslateProcess {
    INSTANCE;
    private static Logger logger = LoggerFactory.getLogger(info.yangguo.waf.request.translate.dubbo.HelloServiceTranslate.class);
    private HelloService helloService;

    HelloServiceTranslate() {
        ReferenceConfig<HelloService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setApplication(new ApplicationConfig("waf-consumer"));
        referenceConfig.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        referenceConfig.setInterface(HelloService.class);
        referenceConfig.setCheck(false);
        helloService = referenceConfig.get();
    }

    @Override
    public String getWafRoutePattern() {
        return ".*";
    }

    @Override
    public HttpResponse execute(String wafRoute, String uri, Map<String, Object> args) {
        HttpResponse result = null;
        if ("dubbo".equals(wafRoute) && uri.endsWith("hello")) {
            String word = helloService.sayHello((String) args.get("name"));
            logger.info("dubbbo hello service result is {}", word);
            result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(word.getBytes()));
        }
        return result;
    }
}
