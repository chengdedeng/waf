package info.yangguo.waf.request.translate.http;

import info.yangguo.waf.Constant;
import info.yangguo.waf.request.translate.TranslateProcess;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Map;

public class Swagger2 implements TranslateProcess {
    private static Logger LOGGER = LoggerFactory.getLogger(Swagger2.class);
    private static HttpClient httpClient;

    static {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        connectionManager.setMaxTotal(10);
        connectionManager.setDefaultMaxPerRoute(10);

        RequestConfig requestConfig = RequestConfig.custom()
                .build();

        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();
    }

    @Override
    public String getWafRoutePattern() {
        return ".*";
    }

    @Override
    public HttpResponse execute(String wafRoute, String uri, Map<String, Object> args) {
        HttpResponse result = null;
        try {
            if (uri.endsWith("swagger-resources/configuration/ui")) {
                String content = "{\"apisSorter\":\"alpha\",\"jsonEditor\":false,\"showRequestHeaders\":false,\"deepLinking\":true,\"displayOperationId\":false,\"defaultModelsExpandDepth\":1,\"defaultModelExpandDepth\":1,\"defaultModelRendering\":\"example\",\"displayRequestDuration\":false,\"docExpansion\":\"none\",\"filter\":false,\"operationsSorter\":\"alpha\",\"showExtensions\":false,\"tagsSorter\":\"alpha\"}";
                result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
            } else if (uri.endsWith("swagger-resources/configuration/security")) {
                String content = "{}";
                result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
            } else if (uri.endsWith("swagger-resources")) {
                String content = "[{\"name\":\"default\",\"url\":\"/v2/api-docs\",\"swaggerVersion\":\"2.0\",\"location\":\"/v2/api-docs\"}]";
                result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
            } else if (uri.endsWith("v2/api-docs")) {
                String content = "{}";
                try {
                    ClassPathResource apiResource = new ClassPathResource("api-docs/" + wafRoute + ".json");
                    content = FileUtils.readFileToString(apiResource.getFile());
                } catch (Exception e) {
                    LOGGER.warn("[{}]'s api docs isn't exist!", wafRoute);
                }
                result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes()));
            } else {
                HttpUriRequest httpUriRequest = new HttpGet("http://127.0.0.1:" + Constant.wafWebConfs.get("server.port") + uri);
                org.apache.http.HttpResponse response = httpClient.execute(httpUriRequest);
                if (200 == response.getStatusLine().getStatusCode()) {
                    try (InputStream inputStream = response.getEntity().getContent()) {
                        byte[] content = IOUtils.toByteArray(inputStream);
                        result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content));
                    }

                }
            }
        } catch (Exception e) {
            result = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
        }
        if (result != null) {
            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            httpHeaders.add("Transfer-Encoding", "chunked");
            result.headers().add(httpHeaders);
        }
        return result;
    }
}
