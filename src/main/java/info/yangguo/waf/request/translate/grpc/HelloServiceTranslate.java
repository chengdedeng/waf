package info.yangguo.waf.request.translate.grpc;

import info.yangguo.waf.request.translate.TranslateProcess;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public enum HelloServiceTranslate implements TranslateProcess {
    INSTANCE;
    private static Logger logger = LoggerFactory.getLogger(info.yangguo.waf.request.translate.grpc.HelloServiceTranslate.class);
    private HelloServiceGrpc.HelloServiceBlockingStub helloService;

    HelloServiceTranslate() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        helloService = HelloServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public String getWafRoutePattern() {
        return ".*";
    }

    @Override
    public HttpResponse execute(String wafRoute, String uri, Map<String, Object> args) {
        HttpResponse result = null;
        if ("grpc".equals(wafRoute) && uri.endsWith("hello")) {
            HelloReply helloReply = helloService.sayHello(HelloRequest.newBuilder().setName((String) args.get("name")).build());
            logger.info("grpc hello service result is {}", helloReply.getMessage());
            result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(helloReply.getMessage().getBytes()));
        }
        return result;
    }
}
