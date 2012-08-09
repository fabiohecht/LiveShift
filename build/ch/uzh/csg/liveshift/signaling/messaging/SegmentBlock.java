package net.liveshift.signaling.messaging;

import java.io.Serializable;

import net.liveshift.configuration.Configuration;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.video.PacketData;

/*
 * this class is used only for exchanging blocks
 * as soon as a peer receives this objects, it writes it on the segmentstorage
 * 
 */

public class SegmentBlock implements Serializable {
	
	private static final long	serialVersionUID	= 4679732501705979677L;
	
	private final SegmentIdentifier segmentIdentifier;
	private final int blockNumber;  //time is calculated based on block and segment number, (durations are Configuration parameters)
	private byte hopCount;  //application-level hop count
	private long downloadTime;
	
	private final PacketData[] packets; //this object contains the actual data
	private final int packetSizeBytes;
	
	public SegmentBlock(final SegmentIdentifier segmentIdentifier, final long timeMillis, final PacketData[] packets) {
		this.segmentIdentifier = segmentIdentifier;
		this.packets = packets;
		this.hopCount = 0;
		this.setDownloadTime();
		
		long segmentStartTimeMS = this.getSegmentIdentifier().getStartTimeMS(); 
		this.blockNumber = ((int)((timeMillis - segmentStartTimeMS)/Configuration.SEGMENTBLOCK_SIZE_MS));
		this.packetSizeBytes = this.calculatePacketSizeBytes(packets);
	}

	/**
	 * this constructor creates an empty block (a block with no data)
	 * 
	 * @param segmentIdentifier
	 * @param blockNumber
	 */
	public SegmentBlock(final SegmentIdentifier segmentIdentifier, final int blockNumber) {
		this.segmentIdentifier = segmentIdentifier;
		this.blockNumber = blockNumber;
		this.packets = new PacketData[0];
		this.packetSizeBytes = 0;
	}
	
	public SegmentBlock(final SegmentIdentifier segmentIdentifier, final int blockNumber, final PacketData[] packets) {
		this.segmentIdentifier = segmentIdentifier;
		this.blockNumber = blockNumber;
		this.packets = packets;
		this.packetSizeBytes = this.calculatePacketSizeBytes(packets);
	}
	
	public SegmentBlock(final SegmentIdentifier segmentIdentifier, final int blockNumber, final PacketData[] packets, final byte hopCount) {
		this(segmentIdentifier,blockNumber,packets);
		this.hopCount=hopCount;
	}

	/**
	 * checks if packet would belong in this block (timewise)
	 * 
	 * @param packetData
	 * @return true if packet would belong in this block, false otherwise
	 */
	public boolean checkTime(PacketData packetData) {
		return ! (packetData.getTimeMS() < this.getTimeMillis() || packetData.getTimeMS() >= this.getTimeMillis() + Configuration.SEGMENTBLOCK_SIZE_MS);
	}

	/**
	 * time is the start time of the block, in seconds
	 * blocknumber should be used as identifier, not the time
	 * 
	 * @return the time of this block
	 */
	public long getTimeMillis() {
		return this.getBlockNumber() * Configuration.SEGMENTBLOCK_SIZE_MS + this.getSegmentIdentifier().getStartTimeMS();
	}
	public int getBlockNumber() {
		return blockNumber;
	}

	public PacketData[] getPackets() {
		return this.packets;
	}

	public static long getStartTimeMillis(long timeMS) {
		return getBlockNumber(timeMS)*Configuration.SEGMENTBLOCK_SIZE_MS + SegmentIdentifier.getSegmentStartTimeMS(SegmentIdentifier.getSegmentNumber(timeMS));
	}
	
	public static int getBlockNumber(long timeMS) {
		long segmentStart = SegmentIdentifier.getSegmentStartTimeMS(SegmentIdentifier.getSegmentNumber(timeMS));
		timeMS -= segmentStart;
		return (int)(timeMS/Configuration.SEGMENTBLOCK_SIZE_MS);
	}
	
	public static long getStartTimeMillis(SegmentIdentifier segmentIdentifier, int blockNumber) {
		long segmentStart = SegmentIdentifier.getSegmentStartTimeMS(segmentIdentifier.getSegmentNumber());
		
		return segmentStart + Configuration.SEGMENTBLOCK_SIZE_MS * blockNumber;
	}

	
	public static int getBlocksPerSegment() {
		return (int)(Configuration.SEGMENT_SIZE_MS/Configuration.SEGMENTBLOCK_SIZE_MS);
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return segmentIdentifier;
	}
	
	public void incrementHopCount() {
		this.hopCount++;
	}
	public byte getHopCount() {
		return this.hopCount;
	}

	@Override
	public int hashCode() {
		return this.segmentIdentifier.hashCode()^this.blockNumber;

	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof SegmentBlock))
			return false;
		SegmentBlock segmentBlock = (SegmentBlock) object;
		return segmentBlock.segmentIdentifier.equals(this.segmentIdentifier) 
			&& segmentBlock.blockNumber==this.blockNumber;
	}
	
	@Override
	public String toString() {
		String out = "ss:"+this.segmentIdentifier.getSubstream()+" b#"+this.blockNumber+" #packets:"+this.packets.length+" hc:"+this.hopCount;
		if (this.segmentIdentifier!=null)
			out += " si:("+this.segmentIdentifier.toString()+")";
		return out;
	}

	public static int getBlocksPerSecond() {
		return 1000/(int)Configuration.SEGMENTBLOCK_SIZE_MS;
	}

	public void setDownloadTime() {
		downloadTime = Clock.getMainClock().getTimeInMillis(false);
	}
	public long getDownloadTime() {
		return downloadTime;
	}

	private int calculatePacketSizeBytes(PacketData[] packetData) {
		int size = 0;
		for (PacketData packet : packetData) {
			size+=packet.getSize();
		}
		return size;
	}

	public int getPacketSizeBytes() {
		return this.packetSizeBytes;
	}
}