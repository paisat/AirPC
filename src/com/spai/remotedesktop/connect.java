package com.spai.remotedesktop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.util.Log;

public class connect {

	DatagramSocket socket=null;
	Socket handShakeSocket=null;
	InetAddress addr;
	int port = 6600;
	int handShakePort=7895;
	private DataInputStream in;
	private DataOutputStream out;
	
	public static int SEND_PASSWORD=0;
	public static int ERROR=1;
	public static int PASSWORD_ACCEPTED=2;
	public static int PASSWORD_REJECTED=3;
	public static int INSESSION=4;
	

	public boolean connectserver(String ipaddress) {
		try {
			Log.v("IPString", ipaddress);
			addr = InetAddress.getByName(ipaddress);
			socket = new DatagramSocket();
			socket.connect(addr, port);

		} catch (UnknownHostException e) {
			Log.v("Unknown Host", "true");
			return false;
		} catch (SocketException e) {
			Log.v("socket", "true");

			return false;
		}
		catch (Exception e) {
			// TODO: handle exception
			Log.v("connect exception",e.getMessage());
		}
		return true;
	}
	
	public boolean setHandShake(String ipAddress)
	{
		try
		{
			handShakeSocket=new Socket(InetAddress.getByName(ipAddress),handShakePort);
			handShakeSocket.setSoTimeout(5000);
		}
		catch(UnknownHostException e)
		{
			return false;
		}
		
		catch(SocketTimeoutException e)
		{
			return false;
		}
		
		catch(IOException e)
		{
			return false;
		}
		
		return true;
		
	}
	
	public int handshakeMessage(String mess)
	{
		
		try
		{
			Log.v("handshake Message",mess);
			
			in=new DataInputStream(handShakeSocket.getInputStream());
		    out=new DataOutputStream(handShakeSocket.getOutputStream());
			
			out.writeUTF(mess);
			
			String inst=in.readUTF();
			Log.v("pass inst",inst+"");
			
			if(inst.equals("send Password"))
				return SEND_PASSWORD;
			else if(inst.equals("password accepted"))
				return PASSWORD_ACCEPTED;
			else if(inst.equals("password rejected"))
				return PASSWORD_REJECTED;
			else if(inst.equals("inSession"))
				return INSESSION;
			
		}
		catch(SocketTimeoutException e)
		{
			
			return ERROR;
		}
		catch(IOException e)
		{
			
			return ERROR;
		}
		
		return ERROR;
	}

	public void send(String s) {
		byte buf[] = new byte[1500];
		buf = s.getBytes();
		try {

			socket.send(new DatagramPacket(buf, buf.length));

		} catch (IOException e) {

		}
	}

	
	
	public void destroy() {
		
		try
		{
			if(socket!=null)
			{
				socket.close();
				socket=null;
			}
			
			if(handShakeSocket!=null)
			{
				handShakeSocket.close();
				handShakeSocket=null;
				out.close();
				in.close();
			}
		}
		catch(IOException e)
		{
			
		}
	}
	
	

}
