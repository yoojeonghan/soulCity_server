package app;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import math.Vector3d;

public class MonsterThread implements ChannelHandler
{
	public float HP = 1.0f;
	public int Power = 300;
	
	public float LocX = -19500.0f;
	public float LocY = -13000.0f;
	public float LocZ = -1613.375366f;
	public double RotY = 300.0f;
	
	public Vector3d MyVector = new Vector3d(LocX, LocY, LocZ);
	static final double RADIAN = 57.29577951;
	public String TargetName = null;
	public boolean IsActive = false;
    public HashedWheelTimer Timer;
	public long lastReadTime = System.currentTimeMillis();
	boolean IsNuckBack = false;
	int RoomNumber = -1;
 	final String KeyString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

	public MonsterThread(int roomNumber) {
		Timer = new HashedWheelTimer();
		RoomNumber = roomNumber;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable arg1) throws Exception {
		System.out.println("[MosterServer]"+ ctx + ", "+ arg1);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	System.out.println("[MosterServer] handlerAdded");
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		System.out.println("[MosterServer] handlerRemoved");
	}
	
	public void ActiveMonster(String targetName) {
		if(!IsActive)
		{
			try {
				TargetName = targetName;
				JSONObject MosterStateObject = new JSONObject();
				
				MosterStateObject.put("RequestNum", "4000");
				MosterStateObject.put("RoomNumber", RoomNumber);
				MosterStateObject.put("MonsterState", 4001);
				  
				String MosterStateString = MosterStateObject.toString();
				InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);
				MosterStateString = Encrypt(KeyString, MosterStateString);
				App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(MosterStateString, CharsetUtil.UTF_8), MyAddress));
				
				System.out.println("[KelsaikNest_MosterServer] Monster Active Message Sending to Game Logic Server.");
			} catch (Exception e) {
				System.out.println("[KelsaikNest_MosterServer] " + e);
			}
			
			Timer.newTimeout(new TimerTask() {
				public void run(Timeout timeout) throws Exception
				{
					long currentTime = System.currentTimeMillis();
					long nextDelay = 0;
					long timeoutMillis = 0L;

					nextDelay = timeoutMillis - (currentTime - lastReadTime);
					
					if (IsNuckBack) {
						timeoutMillis = 3500L;
					} else {
						timeoutMillis = 100L;
					}
					
					if (nextDelay <= 0) {
						if(!IsNuckBack) {
							Timer.newTimeout(this, timeoutMillis, TimeUnit.MILLISECONDS);
						    lastReadTime = System.currentTimeMillis();
						} else {
							Timer.newTimeout(this, timeoutMillis, TimeUnit.MILLISECONDS);
						    lastReadTime = System.currentTimeMillis();
						    IsNuckBack = false;
						}
					} else {
						Timer.newTimeout(this, nextDelay, TimeUnit.MILLISECONDS);
					}					
					MonsterLogic();
				}
			}, 1700, TimeUnit.MILLISECONDS);
			
			Timer.start();
			IsActive = true;
		}
	}
	
	public void AttackMonster(float damage) {
		try {
			Random random = new Random();
			float RandomDamage = random.nextInt(5);
			HP = (float)(HP - (0.01*RandomDamage));
			
			System.out.println(0.01*RandomDamage);
	
			if(HP > 0) {
				JSONObject MosterStateObject = new JSONObject();
					
				MosterStateObject.put("RequestNum", "4000");
				MosterStateObject.put("RoomNumber", RoomNumber);
				MosterStateObject.put("MonsterState", 4003);
				MosterStateObject.put("MonsterHP", HP);
					  
				String MosterStateString = MosterStateObject.toString();
				InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);
					
				MosterStateString = Encrypt(KeyString, MosterStateString);
				App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(MosterStateString, CharsetUtil.UTF_8), MyAddress));
					
				System.out.println("[KelsaikNest_MosterServer] Monster Active Message Sending to Game Logic Server.");
			} else {
				Timer.stop();
					
				JSONObject MosterStateObject = new JSONObject();
					
				MosterStateObject.put("RequestNum", "4000");
				MosterStateObject.put("RoomNumber", RoomNumber);
				MosterStateObject.put("MonsterState", 4004);
					  
				String MosterStateString = MosterStateObject.toString();
				InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);
					
				MosterStateString = Encrypt(KeyString, MosterStateString);
				App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(MosterStateString, CharsetUtil.UTF_8), MyAddress));
					
				System.out.println("[KelsaikNest_MosterServer] Monster die Message Sending to Game Logic Server.");
			}
		} catch(Exception e) {
			System.out.println("[KelsaikNest_MosterServer] " + e);
		}
	}
	
	public void AttackMonster2(float damage) {
		try {
			Random random = new Random();
			float RandomDamage = random.nextInt(10);
			HP = (float)(HP - (0.01*RandomDamage));
			if (HP > 0) {			
				JSONObject MosterStateObject = new JSONObject();
					
				MosterStateObject.put("RequestNum", "4000");
				MosterStateObject.put("RoomNumber", RoomNumber);
				MosterStateObject.put("MonsterState", 4006);
				MosterStateObject.put("MonsterHP", HP);
					
				String MosterStateString = MosterStateObject.toString();
				InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);				
				MosterStateString = Encrypt(KeyString, MosterStateString);
				App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(MosterStateString, CharsetUtil.UTF_8), MyAddress));
			} else {
				Timer.stop();
					
				JSONObject MosterStateObject = new JSONObject();
					
				MosterStateObject.put("RequestNum", "4000");
				MosterStateObject.put("RoomNumber", RoomNumber);
				MosterStateObject.put("MonsterState", 4004);
					  
				String MosterStateString = MosterStateObject.toString();
				InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);
					
				MosterStateString = Encrypt(KeyString, MosterStateString);
				App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(MosterStateString, CharsetUtil.UTF_8), MyAddress));
					
				System.out.println("[KelsaikNest_MosterServer] Monster die Message Sending to Game Logic Server.");
			}
		} catch(Exception e) {
			System.out.println("[KelsaikNest_MosterServer] "+e);
		}
	}
	
	public void MonsterLogic() {
		Vector3d TargetVector = new Vector3d();
		
		try
		{
			for(int i = 0; i < App.PlayerLocationList.size(); i++)
			{
				if(App.PlayerLocationList.get(i).PlayerName.equals(TargetName))
				{
					TargetVector.x = App.PlayerLocationList.get(i).LocationX;
					TargetVector.y = App.PlayerLocationList.get(i).LocationY;
					TargetVector.z = App.PlayerLocationList.get(i).LocationZ;
				}
			}
			
			Vector3d DirectionVector = new Vector3d(TargetVector.x - MyVector.x , TargetVector.y - MyVector.y, 0);
			
			DirectionVector.normalize();
			DirectionVector.scale(50);
			Vector3d LengthVector = new Vector3d(TargetVector.x - MyVector.x , TargetVector.y - MyVector.y, 0);
			Vector3d FirstPlayerVector = new Vector3d(MyVector.getX() - 300.0f, MyVector.getY() + 500.0f, 0);
			Vector3d CurrentDirectionVector = new Vector3d(FirstPlayerVector.getX() - MyVector.getX() , FirstPlayerVector.getY() - MyVector.getY(), 0);
			CurrentDirectionVector.normalize();
			
			Vector3d TargetToDirectionVector = new Vector3d(TargetVector.getX() - MyVector.getX() , TargetVector.getY() - MyVector.getY(), 0);
			TargetToDirectionVector.normalize();
			
			double Angle = CurrentDirectionVector.angle(TargetToDirectionVector);
			System.out.println("[KelsaikNest_MosterServer] Angle : "+ Angle);
			RotY = Math.toDegrees(Angle);
			System.out.println("[KelsaikNest_MosterServer] Degrees : "+ RotY);

			if(LengthVector.dot(FirstPlayerVector) < 0)	{
				RotY = -RotY;
			}
			
			if(LengthVector.length() > 1000) {
				MyVector.add(DirectionVector);
								
				JSONObject MosterStateObject = new JSONObject();

				MosterStateObject.put("RequestNum", "4000");
				MosterStateObject.put("RoomNumber", RoomNumber);
				MosterStateObject.put("MonsterState", 4005);
				MosterStateObject.put("MonsterLocationX", MyVector.getX());
				MosterStateObject.put("MonsterLocationY", MyVector.getY());
				MosterStateObject.put("MonsterLocationZ", MyVector.getZ());
				MosterStateObject.put("MonsterYaw", RotY);
				
				String MosterStateString = MosterStateObject.toString();
				InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);
				
				System.out.println("[KelsaikNest_MosterServer] Monster Yaw : "+ RotY);
				MosterStateString = Encrypt(KeyString, MosterStateString);
				App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(MosterStateString, CharsetUtil.UTF_8), MyAddress));
			} else {
				JSONObject MosterStateObject = new JSONObject();

				MosterStateObject.put("RequestNum", "4000");
				MosterStateObject.put("RoomNumber", RoomNumber);
				MosterStateObject.put("MonsterState", 4002);
				MosterStateObject.put("MonsterYaw", RotY);
				
				String MosterStateString = MosterStateObject.toString();
				InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);
				
				System.out.println("[KelsaikNest_MosterServer] Monster Send String : "+MosterStateString);
				MosterStateString = Encrypt(KeyString, MosterStateString);
				App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(MosterStateString, CharsetUtil.UTF_8), MyAddress));				
			}
		} catch(Exception e) {
			System.out.println("[KelsaikNest_MosterServer]" + e);
		}
	}
	
	public static String Encrypt(String sKey, String sSrc)	{
	    String ivString = "ABCDEF0123456789";
	    
	    while(true)
	    {
	    	if(sSrc.getBytes(CharsetUtil.UTF_8).length % 16 != 0)
	    	{
	    		sSrc += ' ';
	    	}
	    	else
	    	{
	    		break;
	    	}
	    }
	    
	    System.out.println("[KelsaikNest_GameLogicServer_Encrypt] Server sending response :"+sSrc);
	    try
	    {
	        byte[] raw = sKey.getBytes("UTF-8");
	        IvParameterSpec iv = new IvParameterSpec(ivString.getBytes());
	        byte[] PlainText = sSrc.getBytes(CharsetUtil.UTF_8);	        
	        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");  
	        
	        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");     
	        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
	        
	        byte[] byteChiperText = cipher.doFinal(PlainText);
	        String Encrypted = bytesToHexString(byteChiperText);
	        
	        return Encrypted;
	        
	    }
	    catch(Exception e)
	    {
            System.out.println(e.toString());  
            return null;  
	    }	    
	}
	
	public static String Decrypt(String sKey, String sSrc) throws Exception {  
	    String ivString = "ABCDEF0123456789";
	    try {  	            
	        byte[] raw = sKey.getBytes("UTF-8");
	        IvParameterSpec iv = new IvParameterSpec(ivString.getBytes()); 
	        byte[] cipherText = hexStringToByteArray(sSrc);	        
	        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");  
	        
	        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");     
	        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);  
	        	        
	        try {	        	
	        	byte[] original = cipher.doFinal(cipherText);
		        return new String(original).trim();
	        } catch (Exception e) {  
	            System.out.println(e.toString());  
	            return null;  
	        }  
	    } catch (Exception ex) {  
	        System.out.println(ex.toString());  
	        return null;  
	    }  
	}
	
	public static String bytesToHexString(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
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
