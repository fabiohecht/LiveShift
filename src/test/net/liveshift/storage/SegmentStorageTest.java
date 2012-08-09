package net.liveshift.storage;

import java.net.UnknownHostException;

import net.liveshift.core.Channel;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.storage.SegmentStorage;

import org.junit.Test;

import net.liveshift.dummy.*;

public class SegmentStorageTest {
	
	SegmentStorage segmentStorage;

	@Test
	public void testSegmentStorage() throws UnknownHostException {
		segmentStorage = new SegmentStorage(new DummyDht(new DummyPeerId(1)), "/tmp/liveshift-sstest");
		
		SegmentIdentifier segmentIdentifier = new SegmentIdentifier(new Channel("test", (byte)1, 1), (byte)0, 21167168L);
		Segment segment = segmentStorage.getOrCreateSegment(segmentIdentifier, 10);
		
		segment.getSegmentBlockMap().set(66);
		
		SegmentIdentifier segmentIdentifier2 = new SegmentIdentifier(new Channel("test", (byte)1, 1), (byte)0, 21167169L);
		segmentStorage.getOrCreateSegment(segmentIdentifier2, 0);

		
		try {
			Thread.sleep(70000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		segment = segmentStorage.getSegment(segmentIdentifier);
		System.out.println(segment);
	}
}
