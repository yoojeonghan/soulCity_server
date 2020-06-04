package app;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;

public class broadcastServer {
    private int port;
    public static ChannelFuture channelFuture;
    public Channel channel;
    private EventLoopGroup workerGroup;
    public Bootstrap bootstrap;

    public HashedWheelTimer timer;

    public broadcastServer(int port) {
        this.port = port;
        System.out.println("[GameLogicServer] Netty Udp Server Create.");
    }

    public ChannelFuture start() throws InterruptedException {
        workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();

        timer = new HashedWheelTimer();

        bootstrap.group(workerGroup);
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.handler(new ServerChannelInitializer(timer));

        channelFuture = bootstrap.bind(new InetSocketAddress(port)).syncUninterruptibly();
        channel = channelFuture.channel();

        System.out.println("[GameLogicServer] Netty Udp Server Channel Create.");

        return channelFuture;
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        workerGroup.shutdownGracefully();
    }

}