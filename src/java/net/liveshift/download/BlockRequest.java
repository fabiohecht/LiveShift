package net.liveshift.download;

import net.liveshift.configuration.Configuration;
import net.liveshift.storage.SegmentIdentifier;

public class BlockRequest implements Comparable<BlockRequest> {

	final private SegmentIdentifier segmentIdentifier;
	final private int blockNumber;
	
	public BlockRequest(SegmentIdentifier segmentIdentifier, int blockNumber) {
		this.segmentIdentifier = segmentIdentifier;
		this.blockNumber = blockNumber;
	}
	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	public int getBlockNumber() {
		return this.blockNumber;
	}
	
	/*
	 * the order is time-bound. if there's a tie, then lower substream wins
	 * with real svc, it could invert and give priority to lower substream, then lower time 
	 */
	@Override
	public int compareTo(BlockRequest o) {
		if (this.equals(o))
			return 0;
		else {
			long order = this.order() - o.order();
			if (order==0)
				return 0;
			return order>0?1:-1;
		}
	}
	
	private long order() {
		return (this.segmentIdentifier.getSegmentNumber() * (Configuration.SEGMENT_SIZE_MS/Configuration.SEGMENTBLOCK_SIZE_MS) + this.blockNumber) * this.segmentIdentifier.getChannel().getNumSubstreams() + this.segmentIdentifier.getSubstream();
	}
	
	@Override
	public String toString() {
		return "BR(si:"+segmentIdentifier+" b#"+ blockNumber+" order:"+this.order()+")";
	}
	
	@Override
	public int hashCode() {
		return this.segmentIdentifier.hashCode() ^ blockNumber;
	}
	
	@Override
	public boolean equals(Object br1) {
		if (br1 == null || !(br1 instanceof BlockRequest))
			return false;
		BlockRequest br = (BlockRequest) br1;
		return br.segmentIdentifier.equals(this.segmentIdentifier)
				&& br.blockNumber == this.blockNumber;
	}
	
}
