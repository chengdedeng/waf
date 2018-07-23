package info.yangguo.waf.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
@PropertySource("classpath:admin.properties")
@Data
@Component("adminProperties")
public class AdminProperties {
    @Value("${email}")
    public String email;
    @Value("${password}")
    public String password;
}
