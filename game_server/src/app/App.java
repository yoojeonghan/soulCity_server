package app;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import io.netty.channel.ChannelFuture;

public class App
{
    public static HashMap<String, InetSocketAddress> UserAddressHash;
 	public static ArrayList<PlayerLocation> PlayerLocationList;
 	public static ArrayList<String> CurrentUserList;
 	public static ArrayList<String> NonCurrentUser;
 	public static ArrayList<HashMap<String, InetSocketAddress>> DungeonList;
 	public static ArrayList<HashMap<String, Double>> DungeonHPList;
 	
    public static broadcastServer server = null;
    public static matchingClient client = null;
    public static MonsterServer monster = null;
    
    public static ChannelFuture Serverfuture = null;
    public static ChannelFuture Clientfuture = null;
    public static ChannelFuture MosterFuture = null;
        
    public static void main(String[] args) {
        int port = 800;
        
        UserAddressHash = new HashMap<String, InetSocketAddress>();
        DungeonList = new ArrayList<HashMap<String, InetSocketAddress>>();
        DungeonHPList = new ArrayList<HashMap<String, Double>>();
        PlayerLocationList = new ArrayList<PlayerLocation>();
		CurrentUserList = new ArrayList<String>();
		NonCurrentUser = new ArrayList<String>();

        System.out.println("[GameServer] Game Server Start !");
        System.out.println("[GameServer] Netty UDP Server PortNum : " + port);

        monster = new MonsterServer();
    	
        try {
			MosterFuture = monster.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        try {
            server = new broadcastServer(port);
            client = new matchingClient();
            
            Serverfuture = server.start();
            Clientfuture = client.start();
            
            // Wait until the connection is closed.
            Serverfuture.channel().closeFuture().sync();
            Clientfuture.channel().closeFuture().sync();
        	MosterFuture.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            System.err.println("[GameServer] "+ex.getMessage());
        }
    }   
}