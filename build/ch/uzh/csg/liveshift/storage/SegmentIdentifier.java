package net.liveshift.storage;

import java.io.File;
import java.io.Serializable;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.Channel;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.util.Utils;

/**
*
* @author Fabio Victora Hecht, draft
* 
*/
public class SegmentIdentifier implements Serializable, Comparable<SegmentIdentifier> {
	
	private static final long	serialVersionUID	= -1829574076282827774L;
	
	final private long segmentNumber; // start time is calculated based on
										// segment number
	final private byte substream;
	final private Channel channel;

	public SegmentIdentifier(final Channel channel, final byte substream, final long segmentNumber) {
		this.substream=substream;
		this.channel=channel;
		this.segmentNumber = segmentNumber;
	}
	
	public Channel getChannel()
	{
		return channel;
	}

	public byte getSubstream()
	{
		return substream;
	}
	
	public long getStartTimeMS()
	{
		return this.getSegmentNumber() * Configuration.SEGMENT_SIZE_MS;
	}

	public long getEndTimeMS()
	{
		return (this.getSegmentNumber()+1) * Configuration.SEGMENT_SIZE_MS - 1;
	}
	
	public long getBlockStartTimeMs(int block) {
		return this.getStartTimeMS() + (block * Configuration.SEGMENTBLOCK_SIZE_MS);
	}
	public long getBlockEndTimeMs(int block) {
		return this.getStartTimeMS() + (block+1 * Configuration.SEGMENTBLOCK_SIZE_MS) - 1;
	}
	
	//functions for dealing with start/end of segments
	public static long getSegmentStartTimeMS(long segmentNumber) {  //returns time in seconds
		return segmentNumber * Configuration.SEGMENT_SIZE_MS;
	}
	public static long getSegmentNumber (long timeMS) {  //which segment does the given time belong to?
		return timeMS / Configuration.SEGMENT_SIZE_MS;
	}
	public static long getEndTime(long timestamp) {
		return ((timestamp / Configuration.SEGMENT_SIZE_MS) +1) * Configuration.SEGMENT_SIZE_MS - 1;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("");
		if (this.channel==null)
			sb.append("***UNKNOWN CHANNEL***");
		else
			sb.append(this.channel.getName());
		sb.append(",");
		sb.append(this.segmentNumber);
		sb.append(":");
		sb.append(this.substream);
		//sb.append(",time:");
		//sb.append(getStartTimeMS());
		return sb.toString();
	}
	
	public byte[] toByteArray() {
		byte[] out = new byte[13];
		this.toByteArray(out, 0);
		return out;	
	}
	public int toByteArray(byte[] out, int offset) {

		offset = Utils.integerToByteArray(channel.getID(),out,offset);
		out[offset++]=substream;
		offset = Utils.longToByteArray(segmentNumber,out,offset);
		
		return offset;
	}
	
	@Override
	public int hashCode() {
		return channel.hashCode() ^ substream ^ ((int) (segmentNumber ^ (segmentNumber >>> 32)));
	}

	@Override
	public boolean equals(Object si1) {
		if (si1 == null || !(si1 instanceof SegmentIdentifier))
			return false;
		SegmentIdentifier si = (SegmentIdentifier) si1;
		return si.channel.equals(channel) && si.substream == substream
				&& si.segmentNumber == segmentNumber;
	}

	public long getSegmentNumber() {
		return segmentNumber;
	}
	
	static public String getStringIdentifier(Channel channel, long segmentNumber, byte substream) {
		//(we are not writing segment descriptions as files, only blocks)
		String fileName;
		fileName= "c"+Integer.toHexString(channel.hashCode()) +File.separatorChar+ "n" + Long.toHexString(segmentNumber) +File.separatorChar+ "s" + Integer.toHexString(substream);
		
		return fileName;	
	}
	
	public String getNiceStringIdentifier() {
		return this.channel.getName()+"."+this.getSegmentNumber()+"."+this.substream;
	}

	public String getStringIdentifier() {
		return SegmentIdentifier.getStringIdentifier(this.channel, this.getSegmentNumber(), this.substream);
	}
	
	public File getFile(String storageDirectory) {
		return new File(storageDirectory +File.separatorChar+ this.getStringIdentifier());
	}
	
	public File getFile(String storageDirectory, int blockNumber) {
		return new File(storageDirectory +File.separatorChar+ this.getStringIdentifier()+"_b" + Integer.toHexString(blockNumber));
	}
	
	/**
	 * checks if the given block belongs in this segment
	 * 
	 * @param block
	 * @return true if the block belongs in this segment, false otherwise
	 */
	public boolean blockBelongsHere(SegmentBlock block) {
		return (SegmentIdentifier.getSegmentNumber(block.getTimeMillis()) == this.getSegmentNumber());
	}
	
	/**
	 * checks if the given time belongs in this segment
	 * 
	 * @param time in seconds
	 * @return true if the block belongs in this segment, false otherwise
	 */
	public boolean timeBelongsHere(long time) {
		return (SegmentIdentifier.getSegmentNumber(time) == this.getSegmentNumber());
	}

	@Override
	public int compareTo(SegmentIdentifier o) {
		if (this.equals(o))
			return 0;
		if (this.channel.equals(o.channel) ) {
			long orderDif = this.getTimeOrder() - o.getTimeOrder();
			return orderDif>0?1:-1;
		}
		else
			return this.channel.compareTo(o.channel);
		
	}
	private long getTimeOrder() {
		return this.getSegmentNumber() * this.getChannel().getNumSubstreams() + this.substream;
	}
}
