package net.liveshift.video.playbackpolicies;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;

public class CatchUpPlaybackPolicy implements PlaybackPolicy {
	private static final float MINIMUM_SUBSTREAMS_FOR_PLAYING = .8F;  //0..1
	private static final float MINIMUM_BUFFER_FILL = .8F;  //\alpha \in 0..1
	
	final private int numSubstreams;
	final private int initialBufferSizeSeconds;  //buffer B
	private boolean initialBufferDone=false;
	private long startTime = Long.MIN_VALUE;
	private int blocksToPerfect;

	private int[] window;  //the window here looks ahead and sees how much we have buffered 
	private Integer blocksBuffered = 0;
	private int	numBlocksToSkip;

	final private static Logger logger = LoggerFactory.getLogger(PlaybackPolicy.class);

	public CatchUpPlaybackPolicy(byte numSubstreams, int initialBufferSizeSeconds) {
		this.numSubstreams=numSubstreams;
		this.initialBufferSizeSeconds=initialBufferSizeSeconds;
	}

	@Override
	public void clearWindow(long startTime, long perfectPlayTimeMs) {
		this.startTime = startTime;
		this.blocksToPerfect = (int) (1+(perfectPlayTimeMs-startTime)/Configuration.SEGMENTBLOCK_SIZE_MS);
		this.window = new int[Math.min(this.initialBufferSizeSeconds, this.blocksToPerfect)];
	}

	/**
	 * here you feed your blockmaps that need to be monitored
	 * 
	 * @param segment the segment to use for monitoring
	 */	
	@Override
	public void addBlockMap(final Segment segment) {
		
		if (logger.isDebugEnabled()) logger.debug(" addBlockMap("+segment.toString()+")");
		
		if (this.startTime == Long.MIN_VALUE) {
			logger.error("SegmentBlockMonitor must be initialized (reset) before adding block maps");
			LiveShiftApplication.quit("SegmentBlockMonitor must be initialized (reset) before adding block maps");
		}
		
		int startBlockNumber = SegmentBlock.getBlockNumber(this.startTime);
		int blocksPerSegment = SegmentBlock.getBlocksPerSegment();
		
		//gets the part we want from the segmentBlockMap
		BitSet bs = segment.getSegmentBlockMap().get(SegmentBlock.getBlockNumber(Math.max(this.startTime,segment.getSegmentIdentifier().getStartTimeMS())), Math.min(startBlockNumber+this.window.length,blocksPerSegment));
		int shift = Math.max(0, (int) ((segment.getSegmentIdentifier().getStartTimeMS()-this.startTime)/Configuration.SEGMENTBLOCK_SIZE_MS));
		
		//builds window
		int nextBlock = -1;
		synchronized (this.blocksBuffered) {
			
			this.blocksBuffered=0;
			int lastBlock=-1;
			while (nextBlock < this.window.length) {
				
				nextBlock = bs.nextSetBit(nextBlock+1);
				
				if (nextBlock==-1)  //not found || it's the same
					break;
				
				if (nextBlock+shift >= this.window.length)  //past window.length, no sense to go after that
					break;
				
				this.window[nextBlock+shift]++;
				
				if (nextBlock==lastBlock+1 && lastBlock != Integer.MAX_VALUE) {
					this.blocksBuffered++;
					lastBlock=nextBlock;
				}
				else lastBlock=Integer.MAX_VALUE;
			}
		}
	}
	
	@Override
	public PlayingDecision getPlayDecision() {
		int windowPosition=0;
		
		//debug
		if (logger.isDebugEnabled()) {
			String windowString = "";
			for (int i=windowPosition;i<this.window.length;i++)
				windowString += " ["+i+"]="+this.window[i];
			logger.debug("window is"+windowString);
		}
		
		if (this.window.length==0) {
			logger.warn("window is empty, this could be avoided");
			return PlayingDecision.STALL;
		}
		
		if (this.blocksToPerfect<0) {
			logger.warn("avoiding that playback surpasses perfectPlaybackClock");
			return PlayingDecision.STALL;
		}
		
		if (this.blocksToPerfect<=1)
			this.initialBufferDone=false;
		
		//initial buffering
		int bufferedBlocks=this.getBufferedBlocks();
		int bufferBlocks=(int) (this.initialBufferSizeSeconds * Configuration.SEGMENTBLOCK_SIZE_MS/1000);
		if (!this.initialBufferDone && bufferedBlocks < bufferBlocks*MINIMUM_BUFFER_FILL) {
 			logger.debug("Buffering "+(bufferBlocks*MINIMUM_BUFFER_FILL)+" of "+bufferBlocks+" blocks"); 			
 			return PlayingDecision.STALL;
		}
		else
			this.initialBufferDone=true;
		
		//if minimum for playing, play it!
		if (this.window[windowPosition] >= MINIMUM_SUBSTREAMS_FOR_PLAYING * this.numSubstreams)
			return PlayingDecision.PLAY;
		
		//skips to next block, or, if window empty, catches up (and rebuffer)
		for (int i=0; i<this.window.length; i++) {
			if (this.window[i]!=0) {
				//otherwise skip
				this.numBlocksToSkip=i;
				return PlayingDecision.SKIP;
			}
		}
		
		this.numBlocksToSkip=this.blocksToPerfect-1;
		logger.debug("catching up by skipping "+this.numBlocksToSkip+" blocks");

		return PlayingDecision.SKIP;
	}
	
	private int getBufferedBlocks() {
		return this.getBufferedBlocks(this.window.length);
	}
	
	private int getBufferedBlocks(int limit) {
		int playableBlocks=0;
		for (int i=0;i<this.window.length && playableBlocks<limit;i++) {
			if (this.window[i] >= MINIMUM_SUBSTREAMS_FOR_PLAYING * this.numSubstreams) { 
				playableBlocks++;
			}
		}
		return playableBlocks;
	}
	
	/**
	 * returns how much [0..1] from the next block to be played is available
	 * @return how much [0..1] from the next block to be played is available
	 */
	@Override
	public float shareNextBlock() {
		return (float)this.window[0]/this.numSubstreams;
	}

	@Override
	public int getNumBlocksToSkip() {
		return this.numBlocksToSkip;
	}

	@Override
	public int getBlocksBuffered() {
		synchronized (this.blocksBuffered) {
			return this.blocksBuffered;
		}
	}
}
