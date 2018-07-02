package info.yangguo.waf.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Server {
    String ip;
    int port;
    ServerConfig serverConfig;
}
