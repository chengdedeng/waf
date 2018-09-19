package info.yangguo.waf.request.translate.dubbo;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;

import java.io.IOException;

public class Provider {
    public static void main(String[] args) throws IOException {
        ServiceConfig<HelloService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setApplication(new ApplicationConfig("waf-test-provider"));
        serviceConfig.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        serviceConfig.setInterface(HelloService.class);
        serviceConfig.setRef(new HelloServiceImpl());
        serviceConfig.export();
        System.in.read();
    }

    static class HelloServiceImpl implements HelloService {
        @Override
        public String sayHello(String name) {
            return "hello " + name;
        }
    }
}
