package com.songj.websocket.netty;

import com.songj.websocket.handler.MyWebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * netty服务
 */
public class NettyServer {
    private final int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void run() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup parentLoop = new NioEventLoopGroup();
        EventLoopGroup workerLoop = new NioEventLoopGroup();
        try {
            bootstrap.group(parentLoop, workerLoop)
                    .localAddress(port) // 绑定监听端口
                    .channel(NioServerSocketChannel.class) // 设置通道类型:异步socket通道
                    .option(ChannelOption.SO_BACKLOG, 1024) // 最大处理请求数量
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast("codec", new HttpServerCodec())// 设置编码器,包含了HttpServerRequestDecoder,HttpServerResponseEncoder
                                    .addLast("chunked", new ChunkedWriteHandler()) // 持异步写大型数据流，而又不会导致大量的内存消耗
                                    .addLast("content", new HttpObjectAggregator(512*1024))// 解析post请求body参数
                                    .addLast("protocol", new WebSocketServerProtocolHandler("/ws", "WebSocket", true, 65536 * 10))// 对整bai个websocket的通信进行了初du始化，包括握手，以及以后的一些通信控制
                                    .addLast("myHandler", new MyWebSocketHandler());
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            parentLoop.shutdownGracefully();
            workerLoop.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new NettyServer(8282).run();
    }
}
