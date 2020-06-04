package app;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class matchingClient {
	private EventLoopGroup workerGroup;
	private Bootstrap bootstrap;

	public matchingClient() {
		System.out.println("[GameLogicServer] Netty TCP Client Create.");
	}

	public ChannelFuture start() throws InterruptedException {
		workerGroup = new NioEventLoopGroup();
		bootstrap = new Bootstrap();

		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);

		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			protected void initChannel(SocketChannel socketChannel) throws Exception {
				ChannelPipeline pipeline = socketChannel.pipeline();
				pipeline.addLast(new matchingHandler());
				System.out.println("[gameServer] TCP Clinet Socket Init Channel.");
			}
		});

		this.connect("127.0.0.1");
		return null;
	}

	public void connect(final String host) throws InterruptedException {
		ChannelFuture future = bootstrap.connect(host, 802).sync();
		future.channel().closeFuture().sync();

		System.out.println("[gameServer] Netty TCP Client is Connect Matching Server.");
	}

	public void close() {
		if (workerGroup != null) {
			workerGroup.shutdownGracefully();
		}
	}
}