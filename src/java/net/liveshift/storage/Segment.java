package net.liveshift.storage;

import java.io.Serializable;


/**
 * Segment stores local information about a segment
 * 
 * The actual video data is not here: it lies on the disk and it is exchanged through SegmentBlock instances
 * from the SegmentStorage
 * 
 * @author fabio
 *
 */
public class Segment implements Serializable {
	
	private static final long serialVersionUID = 8392931281484935813L;

	final private SegmentIdentifier segmentIdentifier;
	final private SegmentBlockMap segmentBlockMap;  //map representing which blocks we have locally

	private Integer timesGivenUploadSlot = 0;

	public Segment(SegmentIdentifier segmentIdentifier, int startBlockNumber) {
		this.segmentIdentifier = segmentIdentifier;
		this.segmentBlockMap = new SegmentBlockMap(startBlockNumber);
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return segmentIdentifier;
	}

	public SegmentBlockMap getSegmentBlockMap() {
		return segmentBlockMap;
	}
	
	@Override
	public int hashCode() {
		return this.segmentIdentifier.hashCode()^0xFFFFFFFF;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(!(obj instanceof Segment))
			return false;
		
		Segment casted=(Segment)obj;
		return casted.segmentIdentifier.equals(this.segmentIdentifier);
	}
	
	@Override
	public String toString() {
		return "("+this.segmentIdentifier.toString()+") bm:"+this.segmentBlockMap.toString();
	}
	public void incrementTimesGivenUploadSlot() {
		synchronized (this.timesGivenUploadSlot) {
			this.timesGivenUploadSlot++;
		}
	}
	public int getTimesGivenUploadSlot() {
		synchronized (this.timesGivenUploadSlot) {
			return timesGivenUploadSlot;
		}
	}
}
