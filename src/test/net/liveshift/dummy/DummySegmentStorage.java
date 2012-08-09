package net.liveshift.dummy;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.util.Utils;
import net.liveshift.video.PacketData;


public class DummySegmentStorage extends SegmentStorage {

	public DummySegmentStorage(PeerId myId) {
		super(new DummyDht(myId), "/tmp");
	
	}

	@Override
	public void announceAllOnDht() {}
	
	@Override
	public synchronized Segment getSegment(SegmentIdentifier segmentIdentifier) {
		Segment sg  = super.index.get(segmentIdentifier);
		if (sg==null) {
			sg = new Segment(segmentIdentifier, 0);
			sg.getSegmentBlockMap().set(1);
			super.index.put(segmentIdentifier, sg);
		}
		return sg;
	}

	public void setTimesGiven(SegmentIdentifier segmentIdentifier, int times) {
		Segment sg = this.getSegment(segmentIdentifier);
		for (int i=0; i<times; i++)
			sg.incrementTimesGivenUploadSlot();
	}
	
	@Override
	public synchronized SegmentBlock getSegmentBlock(SegmentIdentifier segmentIdentifier, int blockNumber) {
		PacketData[] packets = new PacketData[1];
		packets[0] = new PacketData(Utils.getRandomByteArray(500), 1L, 1L, (byte) 0);
		SegmentBlock sb = new SegmentBlock(segmentIdentifier, blockNumber, packets);
		return sb;
	}
}
