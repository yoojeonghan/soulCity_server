package app;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;

public class ServerChannelInitializer extends ChannelInitializer<DatagramChannel> {	
	public HashedWheelTimer timer;
	
	ChannelPipeline pipeline;

	public ServerChannelInitializer(HashedWheelTimer timer) {
		this.timer = timer;
	}
	
    @Override
    protected void initChannel(DatagramChannel ch) throws Exception {
        pipeline = ch.pipeline();
        
        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, 10));
        pipeline.addLast("Kelsaik_MainHandler", new broadcastHandler(timer, this));
        
        System.out.println("[GameLogicServer] Netty UDP Server Init Channel.");
    }
    
}
