package efagerho.websocket;

import static io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;

import javax.net.ssl.HandshakeCompletedEvent;

import efagerho.websocket.protocol.MessageKind;
import efagerho.websocket.protocol.ServerMessage;
import efagerho.websocket.protocol.ServerStatus;
import efagerho.websocket.protocol.StatusCode;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class DummyHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(DummyHandler.class);

    private final ChannelFutureListener closer = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture f) {
            logger.info("Connection closed");
        }
    };

    private static final byte[] okResponse;

    static {
        ServerStatus okStatus = ServerStatus.newBuilder()
            .setCode(StatusCode.STATUS_CODE_OK)
            .build();
        ServerMessage okMessage = ServerMessage.newBuilder()
            .setKind(MessageKind.MESSAGE_KIND_SERVER_STATUS)
            .setData(okStatus.toByteString())
            .build();
        okResponse = okMessage.toByteArray();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof BinaryWebSocketFrame) {
            ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(okResponse)));
        } else if (frame instanceof TextWebSocketFrame) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(Unpooled.wrappedBuffer(okResponse)));
        } else {
            throw new UnsupportedOperationException("unsupported frame type: " + frame.getClass().getName());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            logger.info("Closing idle connection");
            ctx.close();
        } else if (evt instanceof HandshakeComplete) {
            logger.info("Connection opened");
            ctx.channel().closeFuture().addListener(closer);

        }
    }
}
