package net.liveshift.video;

import java.io.Serializable;

/**
 * PacketData represents one packet
 * 
 * Contains channel, timestamp and sequence number (header infos)
 * and actual video data
 * 
 * @author Cristian Morariu, Fabio Victora Hecht
 *
 */
public class PacketData implements Serializable {

	private static final long	serialVersionUID	= 5554838274588041405L;
	
	private byte[] videoData=null;  //has only the "playable" data, that is, no headers
	
//	private Channel channel;
	private long timeMS;
	private long sequenceNo;
	private byte substream;
	
	public PacketData(final byte[] videoData, final long timeMS, final long sequenceNo, final byte substream) {
		
		this.setVideoData(videoData);
		this.timeMS = timeMS;
		this.sequenceNo = sequenceNo;
//		this.channel = channel;
		this.substream = substream;
	}
		
	private void setVideoData(byte[] videoData) 
	{
		//this.videoData = new byte[videoData.length];
		//System.arraycopy(videoData, 0, this.videoData, 0, videoData.length);
		this.videoData = videoData; 
	}
	public byte[] getVideoData() 
	{
		return this.videoData; 
	}
	
	public long getSequence()
	{
		return sequenceNo;
	}
	
	public long getTimeMS()
	{
		return timeMS;
	} 
	
	public byte getSubstream()
	{
		//if (logger.isDebugEnabled()) logger.debug(sequenceNo+" % "+Configuration.LAYERS_PER_CHANNEL+" == "+sequenceNo % Configuration.LAYERS_PER_CHANNEL);
		return this.substream;
	}
	
	public int getSize() {
		return videoData.length;
	}
	
/*
	private void setChannel(Channel channel) {
		this.channel= channel;
	}

	public Channel getChannel() {
		return channel;
	}
	*/
	@Override
	public String toString() {
		return "t:"+this.timeMS+" sq:"+this.sequenceNo+" packet#"+this.videoData.length;
	}
}
