package net.liveshift.video;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.Channel;
import net.liveshift.storage.SegmentAssembler;
import net.liveshift.time.Clock;

/**
 * Main encoder receiver thread
 * 
 * Listens to incoming (video) data on port Configuration.ENCODER_PORT,
 * wraps it in a PacketData object and sends it to the SegmentAssembler 
 *
 */
public class UdpEncoderReceiver extends Thread implements EncoderReceiver  {

	final private static Logger logger = LoggerFactory.getLogger(UdpEncoderReceiver.class);
	

	private static final int	VIDEO_DATA_LENGTH	= 1600;

	protected final SegmentAssembler segmentAssembler;
	private DatagramSocket socket; 
	protected final Channel channel;
	protected final Clock clock;
	protected final int encoderPort;
	private String	multicastGroup = "";

	protected boolean	running = true;
	protected boolean	actuallyRunning;


	/**
	 * receives UDP packets at encoderPort and makes blocks and segments out of them
	 * 
	 * @param channel
	 * @param segmentAssembler
	 * @param encoderPort
	 */
	public UdpEncoderReceiver(Channel channel, SegmentAssembler segmentAssembler, int encoderPort)
	{
		this.segmentAssembler = segmentAssembler;
		this.clock = Clock.getMainClock();
		this.channel = channel;
		this.encoderPort = encoderPort;
		this.setName("EncoderReceiver");
	}
	
	@Override
	public void startEncoding() {
		this.start();
	}
	
	/**
	 * receives UDP packets at encoderPort from that multicastGroup and makes blocks and segments out of them
	 *  
	 * @param channel
	 * @param segmentAssembler
	 * @param encoderPort
	 * @param multicastGroup
	 */
	public UdpEncoderReceiver(Channel channel, SegmentAssembler segmentAssembler, int encoderPort, String multicastGroup) {
		
		this.segmentAssembler = segmentAssembler;
		this.clock = Clock.getMainClock();
		this.channel = channel;
		this.encoderPort = encoderPort;
		this.multicastGroup  = multicastGroup;
	}

	/* (non-Javadoc)
	 * @see net.liveshift.video.EncoderReceiver#run()
	 */
	@Override
	public void run()
	{
		if (logger.isDebugEnabled()) logger.debug("in run()");
		
		if (this.multicastGroup!=null && this.multicastGroup!="")
			this.runMulticast();
		else
			this.runUnicast();

	}

	private void runUnicast() {

	    try {
			socket = new DatagramSocket(this.encoderPort);
		} catch (SocketException e) {
			
			logger.error("FATAL ERROR not possible to create socket for receiving video ("+e.getMessage()+")");
			e.printStackTrace();
			//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
			
			return;
		}
		
		this.actuallyRunning = true;

		try {
			this.receivePackets(socket);
			socket.close();

		} catch (InterruptedException e) {
			//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
		}
		finally {
		
			this.actuallyRunning = false;
		}
	}
	
	private void runMulticast() {

		MulticastSocket multicastSocket=null;
		InetAddress group=null;
	    try {
	    	group = InetAddress.getByName(this.multicastGroup);
	    	
	    	if (logger.isDebugEnabled()) {
				logger.debug("joining multicast group" + group);
	    	}
	    	
	    	socket = multicastSocket = new MulticastSocket(this.encoderPort);
	    	multicastSocket.joinGroup(group);
		} catch (Exception e) {
			
			logger.error("FATAL ERROR not possible to create socket for receiving video ("+e.getMessage()+")");
			e.printStackTrace();
			//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
			return;
		}
		
		this.actuallyRunning = true;
		
		try {
			try {
				this.receivePackets(socket);
			} catch (InterruptedException e1) {
				//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e1));
			}
			
			try {
				if (!multicastSocket.isClosed()) {
					multicastSocket.leaveGroup(group);
				}
			} catch (IOException e) {
				logger.debug("I/O excepion closing multicast socket, shouldn't be harmful");
			}
			socket.close();
		}
		finally {
			this.actuallyRunning = false;
		}
		
	}
	private void receivePackets(DatagramSocket socket) throws InterruptedException {

		int receivedSize;
		long timestampMS;
		long sequenceNo = 0, lastBlock = 0;
		
		while(this.running)
		{
		    
		    byte[] videoData  = new byte[VIDEO_DATA_LENGTH];
		    DatagramPacket receivePacket=null;
		
		    try {
			    receivePacket = new DatagramPacket(videoData, videoData.length);
			    socket.receive(receivePacket);
		    }
		    catch(Exception e)
		    {
		    	logger.warn("package could not be reveived");
				// TODO Auto-generated catch block
		    	//e.printStackTrace();
		    	//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
		    }
		    
			try {
				receivedSize = receivePacket.getLength();
				timestampMS = this.clock .getTimeInMillis(false);
			    long block = timestampMS / Configuration.SEGMENTBLOCK_SIZE_MS;
			    
			    if (lastBlock<block) {
			    	sequenceNo=0;
				    if (logger.isDebugEnabled()) {
				    	logger.debug("packet time is "+timestampMS);
				    }
			    }
			    else
			    	sequenceNo++;
			    
			    lastBlock =  block;
			    
			    byte[] videoDataRightSize;
			    if (receivedSize==VIDEO_DATA_LENGTH) {
			    	videoDataRightSize=videoData;
			    }
			    else {
			    	videoDataRightSize = new byte[receivedSize];
			    	System.arraycopy(videoData, 0, videoDataRightSize, 0, receivedSize);
			    }
			    
			    PacketData packet = new PacketData(videoDataRightSize,timestampMS,sequenceNo, (byte)(sequenceNo % this.channel.getNumSubstreams()));
			    
			    //sends directly to the segment assembler
			    this.segmentAssembler.putPacket(packet);
			    
			    /*
		    	sleep(2);

			} catch (InterruptedException e) {
				logger.debug("interupted");
				//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
				 */
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
			}
		    
		}		
	}
	/* (non-Javadoc)
	 * @see net.liveshift.video.EncoderReceiver#kill()
	 */
	@Override
	public void kill() {
		this.running = false;
		this.interrupt();
		DatagramSocket socketToClose = this.socket;
		if (socketToClose!=null) {
			socketToClose.close();
		}
		//it needs to wait for the socket to be closed to return, otherwise trying to open it again will fail
		while (this.actuallyRunning)
			try {
				sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
			}
		
	}

	/* (non-Javadoc)
	 * @see net.liveshift.video.EncoderReceiver#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return channel;
	}
}
