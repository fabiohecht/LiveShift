package net.liveshift.video;

import java.util.Date;
import java.util.Random;

import junit.framework.Assert;

import net.liveshift.core.Channel;
import net.liveshift.download.IncomingBlockRateHandler;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.video.playbackpolicies.*;
import net.liveshift.video.playbackpolicies.PlaybackPolicy.PlayingDecision;

import org.junit.Test;


public class PlaybackPolicyTest {
	
	Random rnd = new Random();
	
	@Test
	public void testRatioPlaybackPolicy() {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)0, SegmentIdentifier.getSegmentNumber(new Date().getTime()));
		Segment segment = new Segment(si, 0);
		
		segment.getSegmentBlockMap().set(3, 3+4);
		
		PlaybackPolicy pp = new RatioPlaybackPolicy((byte) 1,0,2);
		
		long startTime = SegmentBlock.getStartTimeMillis(si, 0);
		pp.clearWindow(startTime, startTime+15000);
		pp.addBlockMap(segment);
		
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+1000;
		pp.clearWindow(startTime, startTime+15000);
		pp.addBlockMap(segment);
		
		Assert.assertEquals(PlayingDecision.SKIP, pp.getPlayDecision());
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+3000;
		pp.clearWindow(startTime, startTime+15000);
		pp.addBlockMap(segment);
		
		Assert.assertEquals(PlayingDecision.PLAY, pp.getPlayDecision());
	}
	
	@Test
	public void testRatioPlaybackPolicyWithBuffering() {

		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)0, SegmentIdentifier.getSegmentNumber(new Date().getTime()));
		Segment segment = new Segment(si, 0);
		
		segment.getSegmentBlockMap().set(6, 6+10);

		PlaybackPolicy pp = new RatioPlaybackPolicy((byte) 1,10,2);

		long startTime = SegmentBlock.getStartTimeMillis(si, 0)+3000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
	
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //buffering

		startTime = SegmentBlock.getStartTimeMillis(si, 0)+6000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
	
		Assert.assertEquals(PlayingDecision.PLAY, pp.getPlayDecision());  //buffered
		startTime = SegmentBlock.getStartTimeMillis(si, 0);
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
	
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //no ratio
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+1000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
	
		Assert.assertEquals(PlayingDecision.SKIP, pp.getPlayDecision());  //yes ratio

		startTime = SegmentBlock.getStartTimeMillis(si, 0)+6000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
	
		Assert.assertEquals(PlayingDecision.PLAY, pp.getPlayDecision());  //playable already

	}

	@Test
	public void testSkipThresholdPlaybackPolicyWithBuffering() {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)0, SegmentIdentifier.getSegmentNumber(new Date().getTime()));
		Segment segment = new Segment(si, 0);
		
		segment.getSegmentBlockMap().set(6, 6+10);
		
		PlaybackPolicy pp = new SkipThresholdPlaybackPolicy((byte) 1,10,.5F);
		
		long startTime = SegmentBlock.getStartTimeMillis(si, 0)+3000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //buffering
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+4000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
		Assert.assertEquals(PlayingDecision.SKIP, pp.getPlayDecision());  //buffered, skip
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0);
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);		
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //buffered, stall (beta)
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+6000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
		Assert.assertEquals(PlayingDecision.PLAY, pp.getPlayDecision());  //playable already
		
	}
	
	
	@Test
	public void testRetryPlaybackPolicyWithBuffering() {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)0, SegmentIdentifier.getSegmentNumber(new Date().getTime()));
		Segment segment = new Segment(si, 0);
		
		segment.getSegmentBlockMap().set(6, 6+10);
		
		PlaybackPolicy pp = new RetryPlaybackPolicy((byte) 1,10,10);
		
		long startTime = SegmentBlock.getStartTimeMillis(si, 0)+5000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
		
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //buffering
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+6000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
		
		Assert.assertEquals(PlayingDecision.PLAY, pp.getPlayDecision());  //buffered
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+5000;
		pp.clearWindow(startTime, startTime+16000);
		pp.addBlockMap(segment);
		for (int i=0; i<10; i++) {
			Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //stall 10 times, then skip
		}
		
		Assert.assertEquals(PlayingDecision.SKIP, pp.getPlayDecision());  //skip!
		
	}
	
	@Test
	public void testRdtPlaybackPolicyWithBuffering() {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)0, SegmentIdentifier.getSegmentNumber(new Date().getTime()));
		Segment segment = new Segment(si, 0);
		
		segment.getSegmentBlockMap().set(6, 6+25);
		
		RdtPlaybackPolicy pp = new RdtPlaybackPolicy((byte) 1,10,10);
		
		
		IncomingBlockRateHandler ibrh = new IncomingBlockRateHandler() {			
			@Override
			public void incrementIncomingBlockRate() {
			}			
			@Override
			public float getIncomingBlockRate() {
				return .4F;
			}
		};
		pp.registerIncomingBlockRateHandler(ibrh);
		
		long startTime = SegmentBlock.getStartTimeMillis(si, 0)+5000;
		pp.clearWindow(startTime, startTime+26000);
		pp.addBlockMap(segment);
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //buffering
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+6000;
		pp.clearWindow(startTime, startTime+26000);
		pp.addBlockMap(segment);
		Assert.assertEquals(PlayingDecision.PLAY, pp.getPlayDecision());  //buffered
		
		pp = new RdtPlaybackPolicy((byte) 1,10,10);
		pp.registerIncomingBlockRateHandler(ibrh);
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+5000;
		pp.clearWindow(startTime, startTime+26000);
		pp.addBlockMap(segment);
		Assert.assertEquals(PlayingDecision.STALL, pp.getPlayDecision());  //no rate (stall)
		
		startTime = SegmentBlock.getStartTimeMillis(si, 0)+6000;
		pp.clearWindow(startTime, startTime+26000);
		pp.addBlockMap(segment);
		
		Assert.assertEquals(PlayingDecision.PLAY, pp.getPlayDecision());  //yes rate (play)
	}

}
