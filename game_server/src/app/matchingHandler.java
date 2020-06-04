package app;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONObject;

public class matchingHandler extends ChannelInboundHandlerAdapter {
	final String KeyString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		JSONObject ConnectJSON = new JSONObject();

		ConnectJSON.put("RequestNum", "101");
		String ConnectStr = ConnectJSON.toString();

		System.out.println("[gameServer] TCP Channel Active.");

		ByteBuf messageBuffer = Unpooled.buffer();
		messageBuffer.writeBytes(ConnectStr.getBytes());
		ctx.writeAndFlush(messageBuffer);

		System.out.println("[gameServer] Connect Message Send to Matching Server.");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		String RecvString = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
		RecvString = RecvString.replaceAll("\\p{Space}", "").replaceAll(" ", "").trim();

		try {
			JSONObject RecvObject = new JSONObject(RecvString);
			int AnswerNum = Integer.parseInt((String) RecvObject.get("RequestNum"));

			switch (AnswerNum) {
				case 100:
					System.out.println("[gameServer] Receving Connect Success Message from Matching Server.");
					break;

				case 2002:
					System.out.println("[gameServer] Receving Matching Success Message from Matching Server.");
					System.out.println(RecvObject.toString());

					try {
						JSONArray MatchingUser = (JSONArray) RecvObject.get("UserList");
						ArrayList<String> MatchingUserList = new ArrayList<String>();

						if (MatchingUser != null) {
							int len = MatchingUser.length();

							for (int i = 0; i < len; i++) {
								MatchingUserList.add(MatchingUser.get(i).toString());
							}
						}

						JSONObject CurrentUserObject = new JSONObject();

						CurrentUserObject.put("RequestNum", "5000");
						CurrentUserObject.put("RequestUser", MatchingUserList);

						String CurrentUserString = CurrentUserObject.toString();
						InetSocketAddress MyAddress = new InetSocketAddress("127.0.0.1", 800);

						CurrentUserString = Encrypt(KeyString, CurrentUserString);
						App.server.channel.pipeline().firstContext().writeAndFlush(new DatagramPacket(
								Unpooled.copiedBuffer(CurrentUserString, CharsetUtil.UTF_8), MyAddress));
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;

				default:
					System.out.println("[gameServer] Default Recving Message : " + RecvString);
					break;
			}
		} catch (Exception e) {

		}

	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

	public static String Encrypt(String sKey, String sSrc) {
		String ivString = "ABCDEF0123456789";

		while (true) {
			if (sSrc.getBytes(CharsetUtil.UTF_8).length % 16 != 0) {
				sSrc += ' ';
			} else {
				break;
			}
		}

		System.out.println("[gameServer_Encrypt] Server sending response :" + sSrc);
		try {
			byte[] raw = sKey.getBytes("UTF-8");
			IvParameterSpec iv = new IvParameterSpec(ivString.getBytes());
			byte[] PlainText = sSrc.getBytes(CharsetUtil.UTF_8);

			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

			byte[] byteChiperText = cipher.doFinal(PlainText);
			String Encrypted = bytesToHexString(byteChiperText);

			return Encrypted;

		} catch (Exception e) {
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
		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();

		byte[] data = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}

		return data;
	}
}
