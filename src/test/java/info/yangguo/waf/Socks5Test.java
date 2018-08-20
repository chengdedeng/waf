package info.yangguo.waf;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.CLOSE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * Created with IntelliJ IDEA.
 * User: guo
 * Date: 2017/11/8
 * Time: 下午5:17
 * Description:
 */
public class Socks5Test {
    final static Bootstrap bootstrap = new Bootstrap();
    final static String ua = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";

    static {
        bootstrap.group(new NioEventLoopGroup(5))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpContentDecompressor());
                        pipeline.addLast(new HttpObjectAggregator(10_485_760));
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (msg instanceof FullHttpResponse) {
                                    FullHttpResponse httpResp = (FullHttpResponse) msg;
                                    try {
                                        String strContent = httpResp.content().toString(UTF_8);
                                        System.out.println("body: " + strContent);
                                    } finally {
                                        httpResp.content().release();
                                    }
                                }
                                super.channelRead(ctx, msg);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                cause.printStackTrace(System.err);
                                ctx.close();
                            }
                        });
                        pipeline.addFirst(new Socks5ProxyHandler(new InetSocketAddress("127.0.0.1", 1080)));
                    }
                });
    }

    public static void main(String[] args) throws InterruptedException {
        final String host = "tool.oschina.net";
        final int port = 80;

        for (int i = 0; i < 30; i++) {
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            HttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");
            request.headers().set(HOST, host + ":" + port);
            request.headers().set(USER_AGENT, ua);
            request.headers().set(CONNECTION, CLOSE);

            channelFuture.channel().writeAndFlush(request);
        }
        Thread.sleep(30000000L);
    }
}
