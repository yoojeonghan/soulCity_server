package app;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

public class MonsterServer {
	private EventLoopGroup workerGroup;
	public Bootstrap Monsterbootstrap;
	public static ChannelFuture channelFuture;
	public Channel channel;
	ChannelPipeline pipeline;
	MonsterServer myServer;
	MonsterHandler myHandler;

	public MonsterServer() {
		System.out.println("[KelsaikNest_MosterServer] Monster Server Create.");
		myServer = this;
		myHandler = new MonsterHandler(myServer);
	}

	public ChannelFuture start() throws InterruptedException {
		workerGroup = new NioEventLoopGroup();
		Monsterbootstrap = new Bootstrap();

		Monsterbootstrap.group(workerGroup);
		Monsterbootstrap.channel(NioDatagramChannel.class);

		Monsterbootstrap.handler(new ChannelInitializer<DatagramChannel>() {
			@Override
			protected void initChannel(DatagramChannel DatagramChannel) throws Exception {
				pipeline = DatagramChannel.pipeline();
				pipeline.addLast(myHandler);
				System.out.println("[MosterServer] Monster Server Init Channel.");
			}

		});

		channelFuture = Monsterbootstrap.bind(900);
		channel = channelFuture.channel();

		return channelFuture;
	}

	public void ChannelAddMonster(int roomNumber) {
		try {
			MonsterThread monsterThread = new MonsterThread(roomNumber);
			pipeline.addLast(Integer.toString(roomNumber), monsterThread);
		} catch (IllegalArgumentException e) {
			System.out.println("[MosterServer] " + e);
		}

	}

	public void ChannelWrite(String msg) {
		try {
			System.out.println("[MosterServer] Monster Channel Write");
			InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 900);
			this.channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8), MyAddress));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void ControlMonster(int stateNum, int roomNum, String targetName) {
		switch (stateNum) {
			case 3002:
				MonsterThread monsterThread = (MonsterThread) pipeline.get(Integer.toString(roomNum));
				monsterThread.ActiveMonster(targetName);
				break;
		}
	}

	public void ControlMonster(int stateNum, int roomNum, float damage) {
		MonsterThread monsterThread;

		switch (stateNum) {
			case 3011:
				monsterThread = (MonsterThread) pipeline.get(Integer.toString(roomNum));
				monsterThread.AttackMonster(damage);
				break;

			case 3012:
				System.out.println("[KelsaikNest_MosterServer] CALL AttackMonster2");
				monsterThread = (MonsterThread) pipeline.get(Integer.toString(roomNum));
				monsterThread.IsNuckBack = true;
				monsterThread.AttackMonster2(damage);
				break;
		}
	}

	public void close() {
		if (workerGroup != null) {
			workerGroup.shutdownGracefully();
		}

		if (channel != null) {
			channel.close();
		}
	}
}