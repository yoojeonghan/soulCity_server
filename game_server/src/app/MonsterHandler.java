package app;

import java.util.ArrayList;

import org.json.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

public class MonsterHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	public ChannelHandlerContext MonsterCtx = null;
	MonsterServer MyServer = null;
	public Bootstrap MonsterHandlerbootstrap;
	private NioEventLoopGroup workerGroup;
	ChannelPipeline pipeline;

	ArrayList<MonsterThread> MonsterThreadList = null;

	public MonsterHandler(MonsterServer myServer) {
		MyServer = myServer;
		MonsterThreadList = new ArrayList<MonsterThread>();
	}

	public ChannelFuture start() throws InterruptedException {
		MonsterHandlerbootstrap = new Bootstrap();
		workerGroup = new NioEventLoopGroup();
		MonsterHandlerbootstrap.group(workerGroup);

		return null;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		try {
			channels.add(ctx.channel());
			MonsterCtx = ctx;

			ByteBuf buf = msg.content();
			String RequestStr = buf.toString(CharsetUtil.UTF_8);
			RequestStr = RequestStr.replaceAll("\\p{Space}", "").replaceAll(" ", "").trim();

			JSONObject RequestjsonObj = new JSONObject(RequestStr);
			int RequestNum = Integer.parseInt((String) RequestjsonObj.get("RequestNum"));
			int RoomNumber = Integer.parseInt((String) RequestjsonObj.get("RoomNumber"));
			float Damage = 0;
			switch (RequestNum) {
				case 3002:
					String TargetName = (String) RequestjsonObj.get("RequestUser");
					MyServer.ControlMonster(RequestNum, RoomNumber, TargetName);
					break;

				case 3011:
					Damage = Float.parseFloat((String) RequestjsonObj.get("RequestDamage"));
					MyServer.ControlMonster(RequestNum, RoomNumber, Damage);
					break;

				case 3012:
					Damage = Float.parseFloat((String) RequestjsonObj.get("RequestDamage"));
					MyServer.ControlMonster(RequestNum, RoomNumber, Damage);
					break;
			}

		} catch (Exception e) {
			System.out.println("[KelsaikNest_MosterServer] " + e);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.err.println(cause.getMessage());
		ctx.close();
	}

	@Override
	public boolean acceptInboundMessage(Object msg) throws Exception {
		return super.acceptInboundMessage(msg);
	}

}