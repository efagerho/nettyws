package efagerho.websocket;

import java.util.concurrent.TimeUnit;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static final int PORT = 8080;
    private static final int MAX_MESSAGE_SIZE = 65536;

    public void run() throws Exception {
        final EventLoopGroup bossGroup = new EpollEventLoopGroup();
        final EventLoopGroup workerGroup = new EpollEventLoopGroup();

        try {
            final ServerBootstrap ws = new ServerBootstrap();
            final DummyHandler dummyHandler = new DummyHandler();

            final WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                .checkStartsWith(true)
                .websocketPath("/ws")
                .build();

            logger.info("Starting server...");

            ws.group(bossGroup, workerGroup)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        final ChannelPipeline p = ch.pipeline();
                        p.addLast(new IdleStateHandler(2, 0, 0, TimeUnit.MINUTES));
                        p.addLast(new HttpServerCodec());
                        p.addLast(new HttpObjectAggregator(MAX_MESSAGE_SIZE));
                        p.addLast(new WebSocketServerCompressionHandler());
                        p.addLast(new WebSocketServerProtocolHandler(config));
                        p.addLast(dummyHandler);
                    }
                });

            final ChannelFuture f = ws.bind(PORT).sync();
            logger.info("Listening for connections");

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
    }
}