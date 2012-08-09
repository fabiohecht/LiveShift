package net.liveshift.encoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import net.liveshift.configuration.Configuration.StreamingProtocol;


/**
 * an encoder that simply sends UDP packets at the desired rate to the desired port
 * it actually does not encode anything
 * 
 * @author fabio
 *
 */
public class DummyEncoder extends Thread implements Encoder {
	
	final static int RATE_BIT_PER_SECOND = 25600;
	final static int DATAGRAM_SIZE_BYTES = 1600;

	private InetSocketAddress address;
	private DatagramSocket socket;
	
	private boolean	running;

	@Override
	public void run()
	{
		try {
			this.socket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		this.running = true;
		while (this.running) {
			try {
			
				this.sendDatagram(socket);
				Thread.sleep((int)((1000*DATAGRAM_SIZE_BYTES)/(RATE_BIT_PER_SECOND/8F)));
				
			} catch (InterruptedException e) {
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}	
	}
	
	private void sendDatagram(DatagramSocket socket) throws IOException {
		
		byte[] buf = new byte[DATAGRAM_SIZE_BYTES];
		DatagramPacket packet = new DatagramPacket(buf, DATAGRAM_SIZE_BYTES, this.address);
		socket.send(packet);
	}
	
	@Override
	public void shutdown() {
		this.running = false;

		this.socket.disconnect();
		this.socket.close();
	}

	@Override
	public void startEncoding() {
		this.start();
	}

	/**
	 * streamingProtocol is ignored, always UDP (packets are fake anyway)
	 */
	@Override
	public void createNewStreamingSourceFromFile(String fileName, String destinationIPAddress, int encoderPort, StreamingProtocol streamingProtocol) {
		this.address = new InetSocketAddress(destinationIPAddress, encoderPort);
		
	}

	@Override
	public void createNewStreamingSourceFromDevice(String deviceName, String destinationIPAddress, int encoderPort, StreamingProtocol encoderProtocol) {
		this.address = new InetSocketAddress(destinationIPAddress, encoderPort);
		
	}

}
