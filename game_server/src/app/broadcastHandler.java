package app;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.GlobalEventExecutor;
import math.Vector3d;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class broadcastHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	private static final int HEART_BEAT = 1000;
	private static final int REQ_LOGON = 1001;
	private static final int REQ_LOGOUT = 1002;
	private static final int REQ_CHARSTATE = 1071;
	private static final int REQ_CURRENTUSER = 1003;

	private static final int YES_LOGON = 2001;
	private static final int YES_LOGOUT = 2002;

	private static final int ANS_CURRENTUSER = 1120;
	private static final int ANS_CHARSTATE = 1171;
	private static final int ANS_MATCHING = 5000;

	private static final int REQ_FROMMONSTER = 4000;
	private static final int REQ_MONSTERBEGIN = 3001;
	private static final int REQ_MONSTERATTACK = 3011;
	private static final int REQ_MONSTERATTACK2 = 3012;
	private static final int REQ_USERATTACK = 3021;
	private static final int REQ_ITEMLOOT = 3031;

	public JSONObject CharMoveObject2;
	public JSONArray CharMoveArray;
	final String KeyString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

	boolean IsUserListThread = false;
	IdleStateEvent event = (IdleStateEvent) IdleStateEvent.READER_IDLE_STATE_EVENT;
	public HashedWheelTimer timer;
	public long lastReadTime = System.currentTimeMillis();
	final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	public ServerChannelInitializer serverChannelInitializer;

	public broadcastHandler(HashedWheelTimer timer, ServerChannelInitializer sci) {
		this.timer = timer;
		this.serverChannelInitializer = sci;
	}
	
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {    	    
    		channels.add(ctx.channel());
    		ByteBuf buf = msg.content();
    		String PreRequestStr = buf.toString(CharsetUtil.UTF_8);
    		PreRequestStr = PreRequestStr.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();
    		String RequestStr = Decrypt(KeyString, PreRequestStr);
    		RequestStr = RequestStr.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();    		

			final long timeoutMillis;
			timeoutMillis = 11777L;
			    		
	        try {	        	
	        	JSONObject RequestjsonObj = new JSONObject(RequestStr);
	        	int RequestNum = Integer.parseInt((String)RequestjsonObj.get("RequestNum"));
	        	
	        	if (RequestNum != 1000)  {
		        	System.out.println("[KelsaikNest_GameLogicServer] Server receiving response :" + RequestStr +" From " + msg.sender());
	        	}
	        	
	        	if (RequestNum == ANS_MATCHING) {
            		Matching(ctx, RequestjsonObj);
        	        CurrentUser(ctx);
	        	} else if(RequestNum == REQ_FROMMONSTER) {
	        		AnswerMonsterState(ctx, RequestjsonObj);
	        	} else {
	        		if((String)RequestjsonObj.get("RequestUser") != null) {
			        	String RequestUser = (String)RequestjsonObj.get("RequestUser");
			        	boolean IsLoginUser = false;
			    		for (Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet()) {
			    			if(WaitUserList.getKey().equals(RequestUser)) {
					            App.UserAddressHash.put(RequestUser, msg.sender());
					            IsLoginUser = true;
			    			}
			    		}
			    		
			    		for(int i = 0; i < App.DungeonList.size(); i++)
			    		{
				    		for(Map.Entry<String, InetSocketAddress> DungeonList : App.DungeonList.get(i).entrySet())
				    		{
				    			if(DungeonList.getKey().equals(RequestUser))
				    			{
						            App.DungeonList.get(i).put(RequestUser, msg.sender());
						            IsLoginUser = true;
				    			}
				    		}
			    		}
			    		
			    		if(!IsLoginUser)
			    		{
				            App.UserAddressHash.put(RequestUser, msg.sender());
						}
						
			            switch(RequestNum)
			            {
			            	case HEART_BEAT :
			            		HeartBeat(ctx, RequestUser);
			            		break;
			            		
			            	case REQ_LOGON:
			            		LogIn(ctx, RequestUser, RequestjsonObj);
			            		CurrentUser(ctx);
			            		break;
			            		
			            	case REQ_CHARSTATE:
			            		CharState(ctx, RequestjsonObj, RequestUser);
			            		break;
			                    
			            	case REQ_LOGOUT:
			            		LogOut(ctx, RequestUser);
			            		CurrentUser(ctx);
			            		break;
			            		
			            	case REQ_CURRENTUSER:
			            		CurrentUser(ctx);
			            		break;
			            		
			            	case REQ_MONSTERBEGIN:
			            		MonsterActive(ctx, RequestUser);
			            		break;
			            		
			            	case REQ_MONSTERATTACK:
			            		MonsterAttack(ctx, RequestjsonObj, RequestUser);
			            		break;
			            		
			            	case REQ_MONSTERATTACK2:
			            		MonsterAttack2(ctx, RequestjsonObj, RequestUser);
			            		break;
			            		
			            	case REQ_USERATTACK:
			            		UserAttack(ctx, RequestjsonObj, RequestUser);
			            		break;
			            		
			            	case REQ_ITEMLOOT:
			            		ItemLooting(ctx, RequestUser);
			            		break;
			            }
	        		}
	        	}

	            
	        }
	        catch(Exception e)
	        {
	        	e.printStackTrace();
	        }
	        
	        if(!IsUserListThread)
	        {
	    		timer.newTimeout(new TimerTask() 
	    		{			
	    				public void run(Timeout timeout) throws Exception 
	    				{
	    					long currentTime = System.currentTimeMillis();
	    					long nextDelay = timeoutMillis - (currentTime - lastReadTime);
	    					
	    					if(nextDelay <= 0)
	    					{
	    						timer.newTimeout(this, timeoutMillis, TimeUnit.MILLISECONDS);
	    					    lastReadTime = System.currentTimeMillis( );
	    					}
	    					else
	    					{
	    						timer.newTimeout(this,  nextDelay, TimeUnit.MILLISECONDS);
	    					}
	    					
	    					userEventTriggered(ctx, event);
	    				}
	    				
	    		} , 105937, TimeUnit.MILLISECONDS);
	    		
	    		timer.start();	    		
	    		IsUserListThread = true;
	        }	        
	        
    }
    
    public void LogIn(ChannelHandlerContext ctx, String requestUser, JSONObject requestobj)
    {
    	JSONObject AnswerObj = new JSONObject();
		String AnswerStr = null;
		        
		try {            
			PlayerLocation playerLocation = new PlayerLocation(requestUser,0,0,0,0,0,0);
			App.PlayerLocationList.add(playerLocation);
	        System.out.print("[KelsaikNest_GameLogicServer] Current WaitRoom User List :: ");
	        
			for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet())
			{
	            System.out.print(WaitUserList.getKey());
	            System.out.print("|");
			}
			
	        System.out.println("");
			
			for(int i = 0; i < App.DungeonList.size(); i++)
			{
		        System.out.print("[KelsaikNest_GameLogicServer] Current Dungeon User List. RoomNumber : " + i);

		        for(Map.Entry<String, InetSocketAddress> DungeonUserList: App.DungeonList.get(i).entrySet())
		        {
	                System.out.print(DungeonUserList.getKey());
	                System.out.print("|");
		        }
			}

			System.out.println("");
			
	        try 
	        {
				AnswerObj.put("AnswerNum", YES_LOGON);
		        AnswerObj.put("RequestUser", requestUser);
			} 
	        catch (JSONException e) 
	        {
				e.printStackTrace();
			}

	        AnswerStr = AnswerObj.toString();
	        AnswerStr = AnswerStr.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();
	        AnswerStr = Encrypt(KeyString, AnswerStr);

			for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet())
			{
	            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), WaitUserList.getValue()));
	            System.out.println("[KelsaikNest_GameLogicServer] Server sending response To WaitRoom User : " + WaitUserList.getKey());
			}
			
			for(int i = 0; i < App.DungeonList.size(); i++)
			{
				for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(i).entrySet())
				{
		            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
		            System.out.println("[KelsaikNest_GameLogicServer] Server sending response To Dungeon User : " + DungeonUserList.getKey());
				}
		        
		    	System.out.println("[KelsaikNest_GameLogicServer] Server sending response :"+AnswerStr);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
    }
    
	public void CurrentUser(ChannelHandlerContext ctx)
    {
    	JSONObject AnswerObj = new JSONObject();
    	String AnswerStr = null;
        ArrayList<String> CurrentUserList = new ArrayList<String>();
        
		try 
		{
	        AnswerObj.put("AnswerNum", ANS_CURRENTUSER);
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
        
        for(int i = 0; i < App.PlayerLocationList.size(); i++)
        {
        	CurrentUserList.add(App.PlayerLocationList.get(i).PlayerName);
        }        
        
		try 
        {
			AnswerObj.put("PlayerList", CurrentUserList);
		} 
        catch (JSONException e) 
        {
			e.printStackTrace();
		}         
        
		AnswerStr = AnswerObj.toString();
        AnswerStr = AnswerStr.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();
        AnswerStr = Encrypt(KeyString, AnswerStr);
        
		for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet())
		{
            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), WaitUserList.getValue()));
            System.out.println("[KelsaikNest_GameLogicServer] Server sending response To WaitRoom User : " + WaitUserList.getKey());
		}
            	
    	for(int i = 0; i < App.DungeonList.size(); i++)
    	{
        	JSONObject AnswerObj2 = new JSONObject();
        	String AnswerStr2 = null;
            ArrayList<String> CurrentUserList2 = new ArrayList<String>();
            
    		try 
    		{
    			AnswerObj2.put("AnswerNum", ANS_CURRENTUSER);
    		} 
    		catch (JSONException e) 
    		{
    			e.printStackTrace();
    		}
                		
    		Iterator<String> UserList = App.DungeonList.get(i).keySet().iterator();
    		
    		while(UserList.hasNext())
    		{
    			CurrentUserList2.add(UserList.next());
    		}   
            
    		try 
            {
    			AnswerObj2.put("PlayerList", CurrentUserList2);
    		} 
            catch (JSONException e) 
            {
    			e.printStackTrace();
    		}         
            
    		AnswerStr2 = AnswerObj2.toString();
    		AnswerStr2 = AnswerStr2.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();
	        AnswerStr2 = Encrypt(KeyString, AnswerStr2);
            
    		for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(i).entrySet())
    		{
                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr2, CharsetUtil.UTF_8), DungeonUserList.getValue()));
            	System.out.println("[KelsaikNest_GameLogicServer] Server sending response To Dungeon Room:" + AnswerStr2 + "RoomNumber : " + i);
                System.out.println("[KelsaikNest_GameLogicServer] Server sending response To : " + DungeonUserList.getKey() + " Address : " + DungeonUserList.getValue());
    		}
            
    	}
  
    }
    
    public void HeartBeat(ChannelHandlerContext ctx, String requestUser)
    {

		boolean IsCurrentUser = false;
		for(int i = 0; i < App.CurrentUserList.size(); i++)
		{
			if(App.CurrentUserList.get(i).equals(requestUser))
			{
				IsCurrentUser = true;
			}
		}
		if(!IsCurrentUser)
		{
			App.CurrentUserList.add(requestUser);
		}
		
		for(int i = 0; i < App.CurrentUserList.size(); i++)
		{
			if(!App.CurrentUserList.contains(requestUser))
			{
        		LogOut(ctx, requestUser);
        		CurrentUser(ctx);
			}
		}
    }
    
    public void LogOut(ChannelHandlerContext ctx, String requestUser)
    {
    	String AnswerStr = null;
    	JSONObject AnswerObj = new JSONObject();
		App.UserAddressHash.remove(requestUser);
		
		for(int i = 0; i < App.DungeonList.size(); i++)
		{
			App.DungeonList.get(i).remove(requestUser);
		}
        
		
		for(int i = 0; i < App.PlayerLocationList.size(); i++)
		{
			if(App.PlayerLocationList.get(i).PlayerName.equals(requestUser))
			{
				App.PlayerLocationList.remove(i);
			}
		}
		App.CurrentUserList.remove(requestUser);
		System.out.print("[KelsaikNest_GameLogicServer] Current Login User List : " );

		Set<Entry<String, InetSocketAddress>> set2 = App.UserAddressHash.entrySet();
		Iterator<Entry<String, InetSocketAddress>> itr2 = set2.iterator();
		
		while(itr2.hasNext())
		{
			Map.Entry<String, InetSocketAddress> e = (Map.Entry<String, InetSocketAddress>)itr2.next();
			System.out.print(e.getKey());
			System.out.print("|");
		}

		try 
		{
			AnswerObj.put("AnswerNum", YES_LOGOUT);
			AnswerObj.put("RequestUser", requestUser);
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}

		AnswerStr = AnswerObj.toString();
		AnswerStr = AnswerStr.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();
        AnswerStr = Encrypt(KeyString, AnswerStr);

		for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet())
		{
            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), WaitUserList.getValue()));
            System.out.println("[KelsaikNest_GameLogicServer] Server sending response To WaitRoom User : " + WaitUserList.getKey());
		}
		
		for(int i = 0; i < App.DungeonList.size(); i++)
		{
			for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(i).entrySet())
			{
	            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
	            System.out.println("[KelsaikNest_GameLogicServer] Server sending response To Dungeon User : " + DungeonUserList.getKey());
			}
	        
	    	System.out.println("[KelsaikNest_GameLogicServer] Server sending response :"+AnswerStr);
		}
		
    	System.out.println("[KelsaikNest_GameLogicServer] Server sending response :"+AnswerStr);
    }

    public void CharState(ChannelHandlerContext ctx, JSONObject requestobj, String requestUser)
    {
    	JSONObject AnswerObj = new JSONObject();
    	String AnswerStr = null;    	
		try 
		{
	        AnswerObj.put("AnswerNum", ANS_CHARSTATE);
	        AnswerObj.put("RequestUser", requestUser);
	        AnswerObj.put("StateNum", requestobj.get("StateNum"));
	        
	        if(requestobj.get("StateNum").equals("1"))
	        {
	         	CharMoveObject2 = new JSONObject();
	            ArrayList<String> CurrentUserList = new ArrayList<String>();

	            for(int i = 0; i < App.PlayerLocationList.size(); i++)
	            {
	            	CurrentUserList.add(App.PlayerLocationList.get(i).PlayerName);
	            }
	            
	            for(int i = 0; i < App.PlayerLocationList.size(); i++)
	            {
	            	if(App.PlayerLocationList.get(i).PlayerName.equals(requestUser))
	            	{
	            		try 
	            		{
	            			if(requestobj.get("LocationX") != null)
	            			{
		    					App.PlayerLocationList.get(i).LocationX = Float.parseFloat((String)requestobj.get("LocationX"));
		    	        		App.PlayerLocationList.get(i).LocationY = Float.parseFloat((String)requestobj.get("LocationY"));
		    	        		App.PlayerLocationList.get(i).LocationZ = Float.parseFloat((String)requestobj.get("LocationZ"));	        	
		    	        		App.PlayerLocationList.get(i).RotationYaw = Float.parseFloat((String)requestobj.get("RotationYaw"));
	            			}
	    				} 
	            		catch (NumberFormatException e) 
	            		{
	    					e.printStackTrace();
	    				} 
	            		catch (JSONException e) 
	            		{
	    					e.printStackTrace();
	    				}
	            		break;
	            	}
	            }
	            
	            for(int i = 0; i < App.PlayerLocationList.size(); i++)
	            { 
	                JSONObject CharMoveObject = new JSONObject();        
	                try 
	                {
	                    CharMoveObject.put("LocationX", Float.toString(App.PlayerLocationList.get(i).LocationX));
	    				CharMoveObject.put("LocationY", Float.toString(App.PlayerLocationList.get(i).LocationY));
	    	            CharMoveObject.put("LocationZ", Float.toString(App.PlayerLocationList.get(i).LocationZ));
	    	            CharMoveObject.put("RotationYaw", Float.toString(App.PlayerLocationList.get(i).RotationYaw));
	    	            
	    	            CharMoveObject2.put(App.PlayerLocationList.get(i).PlayerName, CharMoveObject);
	    	            AnswerObj.put("PlayerLocation", CharMoveObject2);
	    			} 
	                catch (JSONException e) 
	                {
	    				e.printStackTrace();
	    			}
	            }	            	            
	        }
	        
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
        AnswerStr = AnswerObj.toString();
        AnswerStr = AnswerStr.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();
        System.out.println("[KelsaikNest_GameLogicServer] @@@@@@@@@@@@@@@@@@@@@@@ Pre Encrypt: " + AnswerStr);
		AnswerStr = Encrypt(KeyString, AnswerStr);
		
		for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet())
		{
            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), WaitUserList.getValue()));
            System.out.println("[KelsaikNest_GameLogicServer] Server sending response To WaitRoom User : " + WaitUserList.getKey());
		}
		
		for(int i = 0; i < App.DungeonList.size(); i++)
		{
			for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(i).entrySet())
			{
	            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
	            System.out.println("[KelsaikNest_GameLogicServer] Server sending response To Dungeon User : " + DungeonUserList.getKey());
			}
		}
    }
    
    public void Matching(ChannelHandlerContext ctx, JSONObject requestobj)
    {
    	try 
    	{
			JSONArray MatchingUser = (JSONArray)requestobj.get("RequestUser");	
			ArrayList<String> MatchingUserArray = new ArrayList<String>();
    		HashMap<String, InetSocketAddress> UserHash = new HashMap<String, InetSocketAddress>();
    		HashMap<String, Double> HPHash = new HashMap<String, Double>();
    		
	    	if(MatchingUserArray != null)
	    	{
	    		int len = MatchingUser.length();
	    		
	    		for(int i = 0; i < len; i++)
	    		{
	    			MatchingUserArray.add(MatchingUser.get(i).toString());
	    		}
	    	}
	    		    	
			for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet())
			{
	        	for(int i = 0; i < MatchingUserArray.size(); i++)
	        	{
	        		if(WaitUserList.getKey().equals(MatchingUserArray.get(i)))
	        		{
	        			UserHash.put(MatchingUserArray.get(i), App.UserAddressHash.get(MatchingUserArray.get(i)));
	        			HPHash.put(MatchingUserArray.get(i), 1.0);
	        		}
	        	}
			}
			
			for(int i = 0; i < MatchingUserArray.size(); i++)
			{
				if(App.UserAddressHash.remove(MatchingUserArray.get(i)) != null);
			}
				        
	        App.DungeonList.add(UserHash);
	        App.DungeonHPList.add(HPHash);
	        
	    	System.out.println("[KelsaikNest_GameLogicServer] Dungeon Create! ");	    	
	    	App.monster.ChannelAddMonster(App.DungeonList.size()-1);
	    	System.out.println("[KelsaikNest_GameLogicServer] Monster Create!! ");	        
	        System.out.print("[KelsaikNest_GameLogicServer] Current WaitRoom User List :: ");
	        
			for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet())
			{
	            System.out.print(WaitUserList.getKey());
	            System.out.print("|");
			}
			
	        System.out.println("");
			
			for(int i = 0; i < App.DungeonList.size(); i++)
			{
		        System.out.print("[KelsaikNest_GameLogicServer] Current "+ i +" Dungeon User List.");

		        for(Map.Entry<String, InetSocketAddress> DungeonUserList: App.DungeonList.get(i).entrySet())
		        {
	                System.out.print(DungeonUserList.getKey());
	                System.out.print("|");
		        }
			}

	        System.out.println("");
	        
		} 
    	catch (JSONException e) 
    	{
			e.printStackTrace();
		}
    }

    public void MonsterActive(ChannelHandlerContext ctx, String requestUser)
    {
    	int RoomNumber = -1;
		System.out.println("[KelsaikNest_GameLogicServer] Receiving to Monster Active Reuqest From " + requestUser);

    	for(int i = 0; i < App.DungeonList.size(); i++) {            
    		if(App.DungeonList.get(i).containsKey(requestUser))
    		{
    			RoomNumber = i;
    			break;
    		}
    	}
    	
    	if(RoomNumber != -1) {
    		System.out.println("[KelsaikNest_GameLogicServer] Activing Monster. Monster RoomNumber is : "+ RoomNumber);
    		
    		try {
        		JSONObject MonsterObject = new JSONObject();
        		
				MonsterObject.put("RequestNum", "3002");
				MonsterObject.put("RoomNumber", Integer.toString(RoomNumber));
				MonsterObject.put("RequestUser", requestUser);
	    		
				String MonsterString = MonsterObject.toString();
	    		App.monster.ChannelWrite(MonsterString);
	    		
	    		System.out.println("[KelsaikNest_GameLogicServer] Request Active to Monster Server. Monster RoomNumber : "+ RoomNumber);
			}  catch (NullPointerException e) {
    			System.out.println("[KelsaikNest_GameLogicServer] " + e);
			}  catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    }
    
	public void AnswerMonsterState(ChannelHandlerContext ctx, JSONObject requestobj) throws InterruptedException {
		try
		{			
			System.out.println(requestobj.toString());
			
			int MonsterState = (int)requestobj.get("MonsterState");
			int RoomNumber = (int)requestobj.get("RoomNumber");
			JSONObject AnswerObject = new JSONObject();
			String AnswerStr = null;
			double MonsterHP;
			
			switch(MonsterState)
			{
				case 4001:
					AnswerObject.put("AnswerNum", "4000");
					AnswerObject.put("MonsterState", MonsterState);
					AnswerStr = AnswerObject.toString();
			        AnswerStr = Encrypt(KeyString, AnswerStr);

					for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(RoomNumber).entrySet())
		    		{
		                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
		                System.out.println("[KelsaikNest_GameLogicServer] Server sending MonsterState To : " + DungeonUserList.getKey() + " Address : " + DungeonUserList.getValue());
		    		}
					break;
					
				case 4002:
					double MonsterYaw2 = (double)requestobj.getDouble("MonsterYaw");
					
					AnswerObject.put("AnswerNum", "4000");
					AnswerObject.put("MonsterState", MonsterState);
					AnswerObject.put("MonsterYaw", MonsterYaw2);
					AnswerStr = AnswerObject.toString();
			        AnswerStr = Encrypt(KeyString, AnswerStr);

					for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(RoomNumber).entrySet())
		    		{
		                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
		                System.out.println("[KelsaikNest_GameLogicServer] ********Server sending MonsterState To : " + DungeonUserList.getKey() + " Address : " + DungeonUserList.getValue());
		    		}
					break;
					
				case 4003:
					MonsterHP = (double)requestobj.get("MonsterHP");
					
					AnswerObject.put("AnswerNum", "4000");
					AnswerObject.put("MonsterState", MonsterState);
					AnswerObject.put("MonsterHP", Double.toString(MonsterHP));
					AnswerStr = AnswerObject.toString();
			        AnswerStr = Encrypt(KeyString, AnswerStr);

					for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(RoomNumber).entrySet())
		    		{
		                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
		                System.out.println("[KelsaikNest_GameLogicServer] Server sending Message " + AnswerStr);
		                System.out.println("[KelsaikNest_GameLogicServer] Server sending MonsterState To : " + DungeonUserList.getKey() + " Address : " + DungeonUserList.getValue());
		    		}
					break;
					
				case 4004:
					AnswerObject.put("AnswerNum", "4000");
					AnswerObject.put("MonsterState", MonsterState);
					AnswerStr = AnswerObject.toString();
			        AnswerStr = Encrypt(KeyString, AnswerStr);

					for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(RoomNumber).entrySet())
		    		{
		                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
		                System.out.println("[KelsaikNest_GameLogicServer] Server sending MonsterState To : " + DungeonUserList.getKey() + " Address : " + DungeonUserList.getValue());
		    		}
					break;
					
				case 4005:
					Vector3d MonsterVector = new Vector3d();
					MonsterVector.x = (double)requestobj.getDouble("MonsterLocationX");
					MonsterVector.y = (double)requestobj.getDouble("MonsterLocationY");
					MonsterVector.z = (double)requestobj.getDouble("MonsterLocationZ");
					double MonsterYaw = (double)requestobj.getDouble("MonsterYaw");
					
					AnswerObject.put("AnswerNum", "4000");
					AnswerObject.put("MonsterState", MonsterState);
					AnswerObject.put("MonsterYaw", MonsterYaw);
					AnswerObject.put("MonsterLocationX", MonsterVector.getX());
					AnswerObject.put("MonsterLocationY", MonsterVector.getY());
					AnswerObject.put("MonsterLocationZ", MonsterVector.getZ());

					AnswerStr = AnswerObject.toString();
			        AnswerStr = Encrypt(KeyString, AnswerStr);

					for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(RoomNumber).entrySet())
		    		{
		                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
		                System.out.println("[KelsaikNest_GameLogicServer] Server sending Message " + AnswerStr);
		    		}
					
					break;
					
				case 4006:
					MonsterHP = (double)requestobj.get("MonsterHP");
					AnswerObject.put("AnswerNum", "4000");
					AnswerObject.put("MonsterState", MonsterState);
					AnswerObject.put("MonsterHP", Double.toString(MonsterHP));
					AnswerStr = AnswerObject.toString();
			        AnswerStr = Encrypt(KeyString, AnswerStr);

					for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(RoomNumber).entrySet())
		    		{
		                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
		                System.out.println("[KelsaikNest_GameLogicServer] Server sending Message " + AnswerStr);
		                System.out.println("[KelsaikNest_GameLogicServer] Server sending MonsterState To : " + DungeonUserList.getKey() + " Address : " + DungeonUserList.getValue());
		    		}
					break;
					
			}
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
	}
    
	public void MonsterAttack(ChannelHandlerContext ctx, JSONObject requestobj, String requestUser) {
    	int RoomNumber = -1;
		System.out.println("[KelsaikNest_GameLogicServer] Receiving to Monster Active Reuqest From " + requestUser);		
		
		for(int i = 0; i < App.DungeonList.size(); i++)
    	{            
    		if(App.DungeonList.get(i).containsKey(requestUser))
    		{
    			RoomNumber = i;
    			break;
    		}
    	}
    	
    	if(RoomNumber != -1)
    	{
    		System.out.println("[KelsaikNest_GameLogicServer] Attack Monster. Monster RoomNumber is : "+ RoomNumber);
    		
    		try {
        		JSONObject MonsterObject = new JSONObject();
				MonsterObject.put("RequestNum", "3011");
				MonsterObject.put("RoomNumber", Integer.toString(RoomNumber));
				MonsterObject.put("RequestDamage", requestobj.get("RequestDamage"));
	    		String MonsterString = MonsterObject.toString();
	    		App.monster.ChannelWrite(MonsterString);
	    		
	    		System.out.println("[KelsaikNest_GameLogicServer] Request Attack to Monster Server. Monster RoomNumber : "+ RoomNumber);
			} catch (NullPointerException e) 
    		{
    			System.out.println("[KelsaikNest_GameLogicServer] " + e);
			} catch (JSONException e) 
    		{
				e.printStackTrace();
			}
    	}
	}
	
	public void MonsterAttack2(ChannelHandlerContext ctx, JSONObject requestobj, String requestUser) {
    	int RoomNumber = -1;
    	
    	for(int i = 0; i < App.DungeonList.size(); i++)
    	{            
    		if(App.DungeonList.get(i).containsKey(requestUser))
    		{
    			RoomNumber = i;
    			break;
    		}
    	}
    	
    	if(RoomNumber != -1)
    	{
    		System.out.println("[KelsaikNest_GameLogicServer] Attack Monster. Monster RoomNumber is : "+ RoomNumber);
    		
    		try 
    		{
        		JSONObject MonsterObject = new JSONObject();
				MonsterObject.put("RequestNum", "3012");
				MonsterObject.put("RoomNumber", Integer.toString(RoomNumber));
				MonsterObject.put("RequestDamage", requestobj.get("RequestDamage"));
	    		String MonsterString = MonsterObject.toString();
	    		App.monster.ChannelWrite(MonsterString);
	    		
	    		System.out.println("[KelsaikNest_GameLogicServer] Request Attack to Monster Server. Monster RoomNumber : "+ RoomNumber);
			} 
    		catch (NullPointerException e) 
    		{
    			System.out.println("[KelsaikNest_GameLogicServer] " + e);
			} 
    		catch (JSONException e) 
    		{
				e.printStackTrace();
			}
    	}
	}
	
	public void UserAttack(ChannelHandlerContext ctx, JSONObject requestobj, String requestUser) {
    	int RoomNumber = -1;

		System.out.println("[KelsaikNest_GameLogicServer] Receiving to Attack User Reuqest. From " + requestUser);
    			
    	for(int i = 0; i < App.DungeonList.size(); i++)  {            
    		if(App.DungeonList.get(i).containsKey(requestUser))
    		{
    			RoomNumber = i;
    			break;
    		}
    	}
    	    	
    	if(RoomNumber != -1) {
    		Double UserHP = App.DungeonHPList.get(RoomNumber).get(requestUser);
    		App.DungeonHPList.get(RoomNumber).put(requestUser, UserHP - 0.05);
    		
    		JSONObject AnswerObject = new JSONObject();
    		
    		try 
    		{
				AnswerObject.put("AnswerNum", "5001");
				AnswerObject.put("RequestUser", requestUser);
				AnswerObject.put("RequestUserHP", App.DungeonHPList.get(RoomNumber).get(requestUser).toString());
			} 
    		catch (JSONException e) 
    		{
				e.printStackTrace();
			}
    		
    		String AnswerStr = AnswerObject.toString();
	        AnswerStr = Encrypt(KeyString, AnswerStr);
    		
    		for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(RoomNumber).entrySet())
    		{
                ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
                System.out.println("[KelsaikNest_GameLogicServer] Server sending Message " + AnswerStr);
    		}
    	}
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		
		Set<Entry<String, InetSocketAddress>> set = App.UserAddressHash.entrySet();
		Iterator<Entry<String, InetSocketAddress>> itr = set.iterator();
		
		ArrayList<String> NonCurrentUser = new ArrayList<String>();
		
		while(itr.hasNext())
		{
			Map.Entry<String, InetSocketAddress> e = (Map.Entry<String, InetSocketAddress>)itr.next();
			
			if(!App.CurrentUserList.contains(e.getKey()))
			{
				NonCurrentUser.add(e.getKey());
				System.out.println(e.getKey()+" is NonCurrentUser. ");
			}
		}
		
		for(int i = 0 ; i < App.DungeonList.size(); i++)
		{
			Set<Entry<String, InetSocketAddress>> set2 = App.DungeonList.get(i).entrySet();
			Iterator<Entry<String, InetSocketAddress>> itr2 = set2.iterator();
			
			while(itr2.hasNext())
			{
				Map.Entry<String, InetSocketAddress> e = (Map.Entry<String, InetSocketAddress>)itr2.next();
				if(!App.CurrentUserList.contains(e.getKey()))
				{
					NonCurrentUser.add(e.getKey());
					System.out.println(e.getKey()+" is NonCurrentUser. ");
				}
			}
		}
		
		for(int i = 0 ; i < NonCurrentUser.size(); i++)
		{
			LogOut(ctx, NonCurrentUser.get(i));
			CurrentUser(ctx);
		}

        App.CurrentUserList.clear();
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

	public void ItemLooting(ChannelHandlerContext ctx, String requestUser)
	{
		try {
			JSONObject AnswerObj = new JSONObject();
			String AnswerStr = null;
	        AnswerObj.put("AnswerNum", REQ_ITEMLOOT);
			AnswerObj.put("RequestUser", requestUser);
			AnswerStr = AnswerObj.toString();
			AnswerStr = AnswerStr.replaceAll("\\p{Space}", "").replaceAll(" ","").trim();
			AnswerStr = Encrypt(KeyString, AnswerStr);
			
			for(Map.Entry<String, InetSocketAddress> WaitUserList : App.UserAddressHash.entrySet()) {
				ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), WaitUserList.getValue()));
				System.out.println("[KelsaikNest_GameLogicServer] Server sending response To WaitRoom User : " + WaitUserList.getKey());
			}
			
			for(int i = 0; i < App.DungeonList.size(); i++) {
				for(Map.Entry<String, InetSocketAddress> DungeonUserList : App.DungeonList.get(i).entrySet()) {
					ctx.write(new DatagramPacket(Unpooled.copiedBuffer(AnswerStr, CharsetUtil.UTF_8), DungeonUserList.getValue()));
				}
			}
		} catch (Exception e) {
			System.out.println("[GameServer]"+ e);
		}
	}
	
	public static String Encrypt(String sKey, String sSrc) {
	    try {
			String ivString = "ABCDEF0123456789";
			while(true) {
				if (sSrc.getBytes(CharsetUtil.UTF_8).length % 16 != 0) {
					sSrc += ' ';
				} else {
					break;
				}
			}
			System.out.println("[KelsaikNest_GameLogicServer_Encrypt] Server sending response (NOT Encrypt) :"+sSrc);
	        byte[] raw = sKey.getBytes("UTF-8");
	        IvParameterSpec iv = new IvParameterSpec(ivString.getBytes());
	        byte[] PlainText = sSrc.getBytes(CharsetUtil.UTF_8);	        
	        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");  
	        
	        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");     
	        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
	        
	        byte[] byteChiperText = cipher.doFinal(PlainText);
	        String Encrypted = bytesToHexString(byteChiperText);
	        
		    System.out.println("[KelsaikNest_GameLogicServer_Encrypt] Server sending response (Encrypt) :"+sSrc);
		    
	        return Encrypted;
	        
	    } catch(Exception e) {
            System.out.println(e.toString());  
            return null;  
	    }	    
	}
	
	public static String Decrypt(String sKey, String sSrc) throws Exception {   
	    try {
			System.out.println("[KelsaikNest_GameLogicServer_Decrypt] Server received response (NOT Decrypt) :"+sSrc);
			String ivString = "ABCDEF0123456789";
	        byte[] raw = sKey.getBytes("UTF-8");
	        IvParameterSpec iv = new IvParameterSpec(ivString.getBytes()); 
	        byte[] cipherText = hexStringToByteArray(sSrc);	        
	        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");  
	        
	        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");     
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(cipherText);
			System.out.println("[KelsaikNest_GameLogicServer_Decrypt] Server received response (Decrypt) :"+new String(original).trim());
			return new String(original).trim();
	    } 
	    catch (Exception ex) 
	    {  
	        System.out.println(ex.toString());  
	        return null;  
	    }  
	}
	
	public static String bytesToHexString(byte[] bytes) {
	        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	        char[] hexChars = new char[bytes.length * 2];
	        int v;
	        for ( int j = 0; j < bytes.length; j++ ) 
	        {
	            v = bytes[j] & 0xFF;
	            hexChars[j * 2] = hexArray[v >>> 4];
	            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	        }
	        return new String(hexChars);
	}  
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    
	    byte[] data = new byte[len / 2];
	    
	    for (int i = 0; i < len; i += 2) 
	    {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
	    }
	    
	    return data;
	}
}