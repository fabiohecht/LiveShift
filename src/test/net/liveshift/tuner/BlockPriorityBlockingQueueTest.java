package net.liveshift.tuner;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.configuration.Configuration;
import net.liveshift.download.BlockPriorityBlockingQueue;
import net.liveshift.download.BlockRequest;
import net.liveshift.download.Candidate;
import net.liveshift.download.Neighbor;
import net.liveshift.download.Tuner;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;

import org.junit.Test;

import net.liveshift.dummy.DummyPeerId;
import net.liveshift.dummy.DummyTuner;


public class BlockPriorityBlockingQueueTest {

	@Test
	public void testSegmentBlockMapTest() {
		
		final int peerCount = 100;
		final int reqCount = 1000;
		final AtomicInteger counter=new AtomicInteger();
		
		final BlockPriorityBlockingQueue<BlockRequest> q = new BlockPriorityBlockingQueue<BlockRequest>();
		
		//creates some fake BlockRequests that this peer must get
		final SegmentIdentifier segmentIdentifier = new SegmentIdentifier(new Channel("testChannel",(byte)1,1),(byte)0,1);

		Thread t0 = new Thread() {
			  @Override
			public void run() {
		
				BlockRequest[] reqs = new BlockRequest[reqCount];
				for (int i = 0; i < reqCount; i++) {
					reqs[i] = new BlockRequest(segmentIdentifier, i);
					
					q.offer(reqs[i]);
					counter.incrementAndGet();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			  }
		};
		t0.start();
		

		//creates some fake neighbors, with block maps saying what they can offer
		final Neighbor[] neighbors = new Neighbor[peerCount];
		final SegmentBlockMap[] bms =  new SegmentBlockMap[peerCount];

		for (int i = 0; i < peerCount; i++) {

			final int chu = i;
			bms[chu] = new SegmentBlockMap();
			
			Thread t1 = new Thread() {
				  @Override
				public void run() {

					  for (int j=0; j< reqCount/peerCount; j++)
						  bms[chu].set(chu*reqCount/peerCount+j);
					  
				  }
				};

			t1.start();
			
			PeerId peerId = new DummyPeerId(i);
			neighbors[i] = new Neighbor(peerId, segmentIdentifier, bms[i], new Candidate(peerId), 5000, new DummyTuner());
			
			for (int j=0; j< reqCount/peerCount; j++) {

				BlockRequest br = null;
				try {
					br = q.take(neighbors[i]);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				counter.decrementAndGet();
	
				System.out.print(br.getBlockNumber()+" ");
				Assert.assertEquals(true, neighbors[i].getBlockMap().get(br.getBlockNumber()));
			}
		}
		Assert.assertEquals(0, counter);

	}

	private int getRandomBlockNumber(int max) {
		return (int) (Math.random()*max);
	}
	
}
