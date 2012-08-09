package net.liveshift.video.playbackpolicies;

import java.util.BitSet;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.video.playbackpolicies.PlaybackPolicy.PlayingDecision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RatioPlaybackPolicy implements PlaybackPolicy {

	private static final float MINIMUM_SUBSTREAMS_FOR_PLAYING = .8F;  //0..1
	private static final float MINIMUM_BUFFER_FILL = .8F;  //0..1
	private final int maxWindowSize;
	
	final private int numSubstreams;
	final private int initialBufferSizeSeconds;
	private boolean initialBufferDone=false;
	private long startTime = Long.MIN_VALUE;
	final private int ratio;
	private int blocksToLive;

	private int[] window;  //the window here looks ahead and sees how much we have buffered 
	private int	numBlocksToSkip;
	private Integer blocksBuffered = 0;

	final private static Logger logger = LoggerFactory.getLogger(PlaybackPolicy.class);

	public RatioPlaybackPolicy(byte numSubstreams, int initialBufferSizeSeconds, int ratio) {
		this.numSubstreams=numSubstreams;
		this.initialBufferSizeSeconds=initialBufferSizeSeconds;
		this.ratio = ratio;
		this.maxWindowSize = Math.max(this.initialBufferSizeSeconds, (int) Math.pow(ratio+1, 2));
		
	}

	@Override
	public void clearWindow(long startTime, long perfectPlayTimeMs) {
		this.startTime = startTime;
		this.blocksToLive = (int) (1+(perfectPlayTimeMs-startTime)/Configuration.SEGMENTBLOCK_SIZE_MS);
		this.window = new int[Math.min(maxWindowSize, (int) (1+(perfectPlayTimeMs-startTime)/Configuration.SEGMENTBLOCK_SIZE_MS))];
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
		
		if (this.blocksToLive<0) {
			logger.warn("avoiding that playback surpasses perfectPlaybackClock");
			return PlayingDecision.STALL;
		}

		//initial buffering
		int bufferBlocks=(int) (this.initialBufferSizeSeconds * Configuration.SEGMENTBLOCK_SIZE_MS/1000);
 		if (!initialBufferDone && getBufferedBlocks(bufferBlocks, (int)Math.ceil(bufferBlocks*MINIMUM_BUFFER_FILL)) < bufferBlocks*MINIMUM_BUFFER_FILL) {
 			logger.debug("Buffering "+(bufferBlocks*MINIMUM_BUFFER_FILL)+" of "+bufferBlocks+" blocks"); 			
			return PlayingDecision.STALL;
		}
		else
			initialBufferDone=true;
		
		//if minimum for playing, play it!
		if (this.window[windowPosition] >= MINIMUM_SUBSTREAMS_FOR_PLAYING * this.numSubstreams)
			return PlayingDecision.PLAY;
		
		//ratio policy
		int ratio = this.ratio, count = 0;
		for (int i=windowPosition;i<this.window.length;i++) {
			if (this.window[i] >= MINIMUM_SUBSTREAMS_FOR_PLAYING * this.numSubstreams)
				count++;
			else
				count-=ratio;
			
			if (count>=0) {
				this.numBlocksToSkip = 1;  //could be improved to the next window[i] but will achieve the same one by one
				logger.debug("count>0 at i="+i+", so skip");
				return PlayingDecision.SKIP;
			}
		}
		return PlayingDecision.STALL;
	}

	private int getBufferedBlocks(int windowLimit, int resultLimit) {
		int playableBlocks=0;
		for (int i=0;i<this.window.length && i<windowLimit && playableBlocks<resultLimit;i++) {
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
