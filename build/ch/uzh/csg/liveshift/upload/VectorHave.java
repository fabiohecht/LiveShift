package net.liveshift.upload;

import net.liveshift.storage.SegmentIdentifier;

/**
 * Vector Haves is optional to substitute regular individual HAVE messages 
 * by a vector that describes the rate which at newer have messages are to
 * be expected. I'm not using this anymore because it seems that while it 
 * saves a bunch of small messages, the overall impact is not that large. 
 * 
 * @author fabio
 *
 */

public class VectorHave {
	
	final private SegmentIdentifier segmentIdentifier;
	final private int blockNumber;
	final private int rate;
	final boolean doHave;

	VectorHave(final SegmentIdentifier segmentIdentifier, final int blockNumber, final boolean doHave, final int rate) {
		this.blockNumber=blockNumber;
		this.doHave=doHave;
		this.rate=rate;
		this.segmentIdentifier = segmentIdentifier;
	}
	
	/**
	 * gets start block (inclusive) of range that is for sure there
	 * @return
	 */
	public int getBlockNumber() {
		return this.blockNumber;
	}
	
	public boolean doHave() {
		return this.doHave;
	}
	
	/**
	 * gets rate at which new blocks are expected to be added after the blockNumber
	 * @return
	 */
	public int getRate() {
		return this.rate;
	}
	
	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	
	@Override
	public String toString() {
		return "VH:("+this.segmentIdentifier+"):"+blockNumber+":"+(doHave?"T":"F")+":"+rate;
	}
}
