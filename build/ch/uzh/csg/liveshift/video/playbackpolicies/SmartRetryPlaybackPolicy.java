package net.liveshift.video.playbackpolicies;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.time.Clock;

public class SmartRetryPlaybackPolicy implements PlaybackPolicy {

	final private static Logger logger = LoggerFactory.getLogger(SmartRetryPlaybackPolicy.class);
	
	private static final float MINIMUM_SUBSTREAMS_FOR_PLAYING = .8F;  //0..1
	private static final float MINIMUM_BUFFER_FILL = .8F;  //0..1

	private static final Float MINIMUM_WAITING_PROBABILITY = .9F;
	
	final private int numSubstreams;
	final private int initialBufferSizeSeconds;
	private boolean initialBufferDone=false;
	final private int maxTimeToWaitTargetSeconds;
	private long startTime = Long.MIN_VALUE;
	private long retriedBlock=-1;
	private long stalledSince=-1;
	private int blocksToPerfect;

	private int[] window;  //the window here looks ahead and sees how much we have buffered 
	private Integer blocksBuffered = 0;

	private Map<Integer, Map<Integer, Float>> lookupTable;

	private int numNeighbors;
	
	public SmartRetryPlaybackPolicy(byte numSubstreams, int initialBufferSizeSeconds, int maxTimeToWaitTargetSeconds) {
		this.numSubstreams=numSubstreams;
		this.initialBufferSizeSeconds=initialBufferSizeSeconds;
		this.maxTimeToWaitTargetSeconds=maxTimeToWaitTargetSeconds;
		
		lookupTable = new TreeMap<Integer, Map<Integer, Float>>();
		
		readLookupTable(SmartRetryPlaybackPolicy.class.getResourceAsStream("/tmp-block-retry-freq-cdf"), lookupTable);
	}

	@Override
	public void clearWindow(long startTime, long perfectPlayTimeMs) {
		this.startTime = startTime;
		this.blocksToPerfect = (int) (1+(perfectPlayTimeMs-startTime)/Configuration.SEGMENTBLOCK_SIZE_MS);
		this.window = new int[this.initialBufferSizeSeconds];
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
	
	public void setNumNeighborsHaveBlock(int numNeighbors) {
		this.numNeighbors = numNeighbors;
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
		
		if (this.blocksToPerfect<1) {
			logger.warn("avoiding that playback surpasses perfectPlaybackClock");
			return PlayingDecision.STALL;
		}

		//initial buffering
		int bufferBlocks=(int) (this.initialBufferSizeSeconds * Configuration.SEGMENTBLOCK_SIZE_MS/1000);
 		if (!initialBufferDone && getBufferedBlocks((int)Math.ceil(bufferBlocks*MINIMUM_BUFFER_FILL)) < bufferBlocks*MINIMUM_BUFFER_FILL) {
 			logger.debug("Buffering "+(bufferBlocks*MINIMUM_BUFFER_FILL)+" of "+bufferBlocks+" blocks"); 			
			this.stalledSince=-1;
			return PlayingDecision.STALL;
		}
		else
			initialBufferDone=true;
		
		//if minimum for playing, play it!
		if (this.window[windowPosition] >= MINIMUM_SUBSTREAMS_FOR_PLAYING * this.numSubstreams) {
			this.stalledSince=-1;
			return PlayingDecision.PLAY;
		}
		
		//retries (stalls) retries times, then skips
		if (this.retriedBlock == this.startTime/Configuration.SEGMENTBLOCK_SIZE_MS) {
			int stalledForMillis = (int) (Clock.getMainClock().getTimeInMillis(false) - this.stalledSince);

			boolean thereIsABlockAhead = this.getBufferedBlocks(1)>0;
			
			boolean shouldWait = !thereIsABlockAhead?true:(!(stalledForMillis > this.maxTimeToWaitTargetSeconds*1000) && this.shouldWait(this.numNeighbors, this.maxTimeToWaitTargetSeconds));

			if (!shouldWait) {
				logger.debug("gave up retrying after "+stalledForMillis+"/"+this.maxTimeToWaitTargetSeconds*1000+" ms");
				return PlayingDecision.SKIP;
			}				
			logger.debug("retrying for "+stalledForMillis+"/"+this.maxTimeToWaitTargetSeconds*1000+" ms");
		}
		else {  //first stall of block
			this.retriedBlock = this.startTime/Configuration.SEGMENTBLOCK_SIZE_MS;
			this.stalledSince = Clock.getMainClock().getTimeInMillis(false);

			logger.debug("retrying for 1st time");

		}

		return PlayingDecision.STALL;
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
		return 1;
	}

	@Override
	public int getBlocksBuffered() {
		synchronized (this.blocksBuffered) {
			return this.blocksBuffered;
		}
	}


	private void readLookupTable(InputStream inputStream, Map<Integer, Map<Integer, Float>> map) {

		BufferedReader bufRdr = null;
		try {
			bufRdr = new BufferedReader(new InputStreamReader(inputStream));
			
			String line = null;
			String[] splitline;
			 
			//read each line of text file
			while((line = bufRdr.readLine()) != null)
			{
				splitline = line.split(" |;|\t");
				if (splitline.length==3) {
					Integer key1 = Integer.valueOf(splitline[0]);
					Map<Integer,Float> map2 = map.get(key1);
					if (map2==null) {
						map2 = new HashMap<Integer, Float>();
						map.put(key1, map2);
					}
					
					map2.put(Integer.valueOf(splitline[1]), Float.valueOf(splitline[2]));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			LiveShiftApplication.quit("file not found");
		} catch (IOException e) {
			e.printStackTrace();
			LiveShiftApplication.quit("ioexception");
		}
		finally {
			//close the file
			try {
				if (bufRdr!=null)
					bufRdr.close();
			} catch (IOException e) {
				e.printStackTrace();
				LiveShiftApplication.quit("ioexception2");
			}
		}		
	}
	
	private boolean shouldWait(Integer numNeighbors, Integer maxTimeSeconds) {
		
		Map<Integer, Float> map2 = this.lookupTable.get(numNeighbors);
		
		if (map2==null) {
			//I don't know, no corresponding entry on table
			logger.warn("no corresponding entry on table 1 for #N="+numNeighbors+" t="+maxTimeSeconds);
			return false;  //TODO true or false?? I put false, because these will happen only for extreme values of either parameter
		}
		else {
			Float p = map2.get(maxTimeSeconds);
			if (p==null) {
				//I don't know, no corresponding entry on table
				logger.warn("no corresponding entry on table 2 for #N="+numNeighbors+" t="+maxTimeSeconds);
				return false;
			}
			else {
				logger.debug("#N="+numNeighbors+" t="+maxTimeSeconds+" p="+p+" p_min=" + MINIMUM_WAITING_PROBABILITY);
				return p > MINIMUM_WAITING_PROBABILITY;
			}
		}
		
	}
}
