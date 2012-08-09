package net.liveshift.video;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.Channel;
import net.liveshift.storage.SegmentAssembler;


public class TcpEncoderReceiver extends UdpEncoderReceiver {
	final private static Logger logger = LoggerFactory.getLogger(TcpEncoderReceiver.class);

	private static final int	VIDEO_DATA_LENGTH	= 10000;

	private ServerSocket serverSocket; 

	public TcpEncoderReceiver(Channel channel, SegmentAssembler segmentAssembler, int encoderPort) {
		super(channel, segmentAssembler, encoderPort);
	}

	@Override
	public void run()
	{
		if (logger.isDebugEnabled()) logger.debug("in run()");
		
		while (this.running) {
			Socket socket=null;
		    try {
				serverSocket = new ServerSocket(this.encoderPort);
				
				if (logger.isDebugEnabled()) logger.debug("TCP ServerSocket waiting for connection");
				
				socket = serverSocket.accept();
	
			} catch (IOException e) {
				
				logger.error("FATAL ERROR not possible to create socket for receiving video ("+e.getMessage()+")");
				e.printStackTrace();
				
				return;
			}
			
			this.actuallyRunning = true;
	
			try {
				this.receivePackets(socket);
				serverSocket.close();
	
			} catch (IOException e) {
	
				logger.error("FATAL ERROR not possible to close socket for receiving video ("+e.getMessage()+")");
				e.printStackTrace();
				
			} catch (InterruptedException e) {
		
			}
			finally {
				if (!this.running)
					this.actuallyRunning = false;
			}
		}
	}

	private void receivePackets(Socket socket) throws InterruptedException, IOException {

		long timestampMS;
		long sequenceNo = 0, lastBlock = 0;
		
		InputStream inputStream = socket.getInputStream();
		
		while(this.running)
		{
		    
		    byte[] videoData  = new byte[VIDEO_DATA_LENGTH];
		    int bytesRead = 0;
		    
		    try {
		    	bytesRead = inputStream.read(videoData);
		    }
		    catch(Exception e)
		    {
				// TODO Auto-generated catch block
		    	e.printStackTrace();
		    }
		    
		    if (bytesRead==-1) {
		    	//disconnected
		    	return;
		    }
		    
			try {
				timestampMS = this.clock.getTimeInMillis(false);
			    long block = timestampMS / Configuration.SEGMENTBLOCK_SIZE_MS;
			    
			    if (logger.isDebugEnabled()) logger.debug("packet time is "+timestampMS);
			    
			    if (lastBlock<block)
			    	sequenceNo=0;
			    else
			    	sequenceNo++;
			    
			    lastBlock =  block;
			    
			    byte[] videoDataRightSize;
			    if (bytesRead==VIDEO_DATA_LENGTH)
			    	videoDataRightSize=videoData;
			    else {
			    	videoDataRightSize = new byte[bytesRead];
			    	System.arraycopy(videoData, 0, videoDataRightSize, 0, bytesRead);
			    }
			    PacketData packet = new PacketData(videoDataRightSize,timestampMS,sequenceNo, (byte)(sequenceNo % this.channel.getNumSubstreams()));
			    
			    //sends directly to the segment assembler
			    this.segmentAssembler.putPacket(packet);
			    
		    	sleep(2);

			} catch (InterruptedException ie) {
				logger.debug("interrupted -- shutting down");
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		}		
	}
	
	@Override
	public void kill() {
		this.running = false;
		this.interrupt();
		
		try {
			this.serverSocket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//it needs to wait for the socket to be closed to return, otherwise trying to open it again will fail
		while (this.actuallyRunning)
			try {
				sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
}
