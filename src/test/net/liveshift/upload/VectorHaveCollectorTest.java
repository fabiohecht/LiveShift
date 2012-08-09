package net.liveshift.upload;


import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Random;
import java.util.logging.LogManager;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import net.liveshift.core.Channel;
import net.liveshift.download.ProbabilisticSegmentBlockMap;
import net.liveshift.download.Tuner;
import net.liveshift.p2p.Peer;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.upload.VectorHave;
import net.liveshift.upload.VectorHaveCollectors;
import net.liveshift.upload.VectorHaveCollector.VectorHaveSender;

import org.junit.Test;

import net.liveshift.dummy.DummyPeerId;

public class VectorHaveCollectorTest {
	
	final private Random rnd = new Random();

	// Init LogManager
	static {
		try {
			LogManager.getLogManager().readConfiguration(Peer.class.getResourceAsStream("/jdklogtest.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testVectorHave() throws InterruptedException, UnknownHostException {

		Channel c = new Channel("testChannel", (byte)1, 0);
		Segment localSegment = new Segment(new SegmentIdentifier(c, (byte)0, 666), 0);
		Segment remoteSegment = new Segment(new SegmentIdentifier(c, (byte)0, 666), 0);
		
		Tuner tuner;
		final ProbabilisticSegmentBlockMap psbm = new ProbabilisticSegmentBlockMap(remoteSegment.getSegmentBlockMap(), remoteSegment.getSegmentIdentifier(), new DummyPeerId(1), tuner);
		final SegmentBlockMap lsbm = localSegment.getSegmentBlockMap();
		
		VectorHaveCollectors colls = new VectorHaveCollectors(new VectorHaveSender() {
			
			private int	counter=0;

			@Override
			public void sendHaveMessage(VectorHave vectorHave) {
				System.out.println("==> will send: "+vectorHave+ " #"+(++this.counter));
				
				psbm.addSegmentBlockMapUpdateVector(vectorHave);
				
				//TODO needs to take into consideration that there's a queue to absorb if prediction is a bit too early
				
				System.out.println(lsbm+"=="+psbm);
				try {
					Assert.assertTrue(psbm.equals(lsbm));
				}
				catch (AssertionFailedError e) {
					System.err.println("Assertion Error! "+e);
				}
				
			}

			@Override
			public void sendHaveMessageToNewSubscribers(VectorHave vectorHave) {
				// TODO Auto-generated method stub
				
			}
		}, false);
		
		//tries a well-rated sequential stream
		for (int i=100; i<115; i++) {
			System.out.println("adding "+i);
			lsbm.set(i);
			colls.blockReceived(localSegment, i);
			Thread.sleep((long) (i<110?100:i>120?200:100+this.rnd.nextFloat()*100)*10);
		}
		System.out.println("----------------------------------");
		
		//makes things bad now
		BitSet blocksLeft = new BitSet();
		blocksLeft.set(150,200);
		while (!blocksLeft.isEmpty()) {
			int block = this.rnd.nextInt(50)+150;
			if (blocksLeft.get(block)) {
				blocksLeft.set(block, false);

				System.out.println("adding "+block);
				lsbm.set(block);
				colls.blockReceived(localSegment, block);

				Thread.sleep(10);
			}
		}
		System.out.println("----------------------------------");
		
		//double thread, each one well-paced
		for (int i=200; i<250; i++) {
			System.out.println("adding "+i);
			lsbm.set(i);
			colls.blockReceived(localSegment, i);
			Thread.sleep((long) (this.rnd.nextFloat()*100));
			
			System.out.println("adding "+(i+50));
			lsbm.set(i+50);
			colls.blockReceived(localSegment, (i+50));
			Thread.sleep((long) (this.rnd.nextFloat()*100));
		}
		
	}
	

	@Test
	public void testVectorHave2() throws InterruptedException {

		Channel c = new Channel("testChannel", (byte)1, 0);
		Segment si = new Segment(new SegmentIdentifier(c, (byte)0, 666), 0);
		
		final ProbabilisticSegmentBlockMap psbm = new ProbabilisticSegmentBlockMap(new SegmentBlockMap(), si.getSegmentIdentifier(), new DummyPeerId(1));
		final SegmentBlockMap lsbm = si.getSegmentBlockMap();
		
		VectorHaveCollectors colls = new VectorHaveCollectors(new VectorHaveSender() {
			
			private int	counter=0;

			@Override
			public void sendHaveMessage(VectorHave vectorHave) {
				System.out.println("==> will send: "+vectorHave+ " #"+(++this.counter));
				
				psbm.addSegmentBlockMapUpdateVector(vectorHave);
				
				System.out.println(lsbm+"=="+psbm);
				try {
					Assert.assertTrue(psbm.equals(lsbm));
				}
				catch (AssertionFailedError e) {
					System.err.println("Assertion Error! "+e);
				}
				
			}

			@Override
			public void sendHaveMessageToNewSubscribers(VectorHave vectorHave) {
				// TODO Auto-generated method stub
				
			}
		}, false);
		
		//tries a stream with holes
		for (int i=100; i<120; i+=2) {
			System.out.println("adding "+i);
			lsbm.set(i);
			colls.blockReceived(si, i);
			Thread.sleep((long) (i<110?100:i>120?200:100+this.rnd.nextFloat()*100));
		}
		
	}
	@Test
	public void testVectorHave3() throws InterruptedException {

		Channel c = new Channel("testChannel", (byte)1, 0);
		Segment si = new Segment(new SegmentIdentifier(c, (byte)0, 666), 0);
		
		final ProbabilisticSegmentBlockMap psbm = new ProbabilisticSegmentBlockMap(new SegmentBlockMap(), si.getSegmentIdentifier(), new DummyPeerId(1));
		final SegmentBlockMap lsbm = si.getSegmentBlockMap();
		
		VectorHaveCollectors colls = new VectorHaveCollectors(new VectorHaveSender() {
			
			private int	counter=0;

			@Override
			public void sendHaveMessage(VectorHave vectorHave) {
				System.out.println("==> will send: "+vectorHave+ " #"+(++this.counter));
				
				psbm.addSegmentBlockMapUpdateVector(vectorHave);
				
				System.out.println(lsbm+"=="+psbm);
				try {
					Assert.assertTrue(psbm.equals(lsbm));
				}
				catch (AssertionFailedError e) {
					System.err.println("Assertion Error! "+e);
				}
				
			}

			@Override
			public void sendHaveMessageToNewSubscribers(VectorHave vectorHave) {
				// TODO Auto-generated method stub
				
			}
		}, false);
		
		//tries a specific one
		//[237,-];[236,-902];[235,-722];[234,-1755]
		for (int i=234; i<238; i++) {
			System.out.println("adding "+i);
			lsbm.set(i);
			colls.blockReceived(si, i);
			long t=100000;
			switch (i) {
				case 234: t=1755; break;
				case 235: t=722; break;
				case 236: t=902; break;
			}
			Thread.sleep(t);
		}
		
	}
}
