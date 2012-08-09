package net.liveshift.video;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 *
 * @author Cristian Morariu, Fabio Victora Hecht
 */
public class VideoPlayerSender {

	private DatagramSocket socket;
	final private String	localIpAddress;
	final private int	playerPort;
	
	public VideoPlayerSender(final String localIpAddress, final int playerPort) {

		this.localIpAddress=localIpAddress;
		this.playerPort=playerPort;
		
		socket = null;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void sendDataToPlayer(byte[] buf) throws IOException
	{
//		CLI.log("send data to player: " + buf.length);
		InetAddress address;
		address = InetAddress.getByName(this.localIpAddress);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, this.playerPort);
		socket.send(packet);

	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		
		this.socket.close();
	}
}
