package net.liveshift.download;

import java.util.BitSet;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.upload.UploadSlot;
import net.liveshift.util.TriggableScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * There is one BlockScheduler per tuned segment (i.e. one per substream)
 * At the end of a segment, the next set will be crated
 * 
 * The goal of the BlockSchedules is to select which blocks must be fetched
 * The selected blocks are put into a priority queue, from which the BlockRequester(s) will get and request
 *  
 */
public class BlockScheduler {

	final private static Logger logger = LoggerFactory.getLogger(BlockScheduler.class);

	static private final long BLOCK_SELECT_FREQUENCY_MS	= Configuration.SEGMENTBLOCK_SIZE_MS;
	static private final long MINIMUM_FREQUENCY_MS	= Configuration.SEGMENTBLOCK_SIZE_MS/5L;
	static private final int NUMBER_OF_BLOCKS_TO_ENQUEUE = 15;
	static private final float SCHEDULE_NEXT_SEGMENT_WHEN_REQUESTED_SHARE = .94F;
	private static final int MAX_BLOCKS_AHEAD_OF_PLAY_TIME = SegmentBlock.getBlocksPerSecond()*30;

	final private Segment segment;
	final private LiveShiftApplication liveShiftApplication;

	private TriggableScheduledExecutorService scheduledExecutorService;

	final private AtomicBoolean nextSegmentRequested = new AtomicBoolean(false);
	boolean firstBlockScheduled = false;
	private long highestScheduledBlockTime;
	boolean running = true;
	
	//schedules block playback
	final private Callable<Boolean> scheduledTask = new Callable<Boolean>() {
		
		@Override
		public Boolean call() throws Exception {
			try {
				if (running)
					return addBlocksToQueue(selectBlocks());
			}
			catch (Exception e)
			{
				//just so it doesn't die silently if an unhandled exception happened
				e.printStackTrace();
			}
			
			return false;
		}
	};

	public BlockScheduler(Segment segment, NeighborList neighborList, LiveShiftApplication liveShiftApplication) {

		if (logger.isDebugEnabled()) logger.debug("in constructor (sd:" + segment.getSegmentIdentifier().toString() + ")");

		this.segment = segment;
		this.liveShiftApplication = liveShiftApplication;
		
		this.scheduledExecutorService = new TriggableScheduledExecutorService(this.scheduledTask, 100, MINIMUM_FREQUENCY_MS, BLOCK_SELECT_FREQUENCY_MS, Clock.getMainClock(),"BlockScheduler ["+segment.getSegmentIdentifier().getSegmentNumber()+":"+segment.getSegmentIdentifier().getSubstream()+"]");
		
	}
	
	void shutdown() {

		if (logger.isDebugEnabled()) logger.debug("shutting down. goodbye!");
		
		this.running = false;
		
		if (this.scheduledExecutorService != null)
			this.scheduledExecutorService.shutdown();
	}

	private BitSet selectBlocks() {

		// this is where the download policy must be set (block selection)

		if (logger.isDebugEnabled()) logger.debug("in selectBlocks");
		BitSet nextBlocks = null;

		//check
		if (!this.segment.getSegmentIdentifier().equals(this.segment.getSegmentIdentifier())) {
			logger.error("SegmentStorage is giving away wrong segments!! ("+this.segment.getSegmentIdentifier()+"!="+this.segment.getSegmentIdentifier()+")");
			LiveShiftApplication.quit("SegmentStorage is giving away wrong segments!!");
		}
		
		// at the moment: tries the next NUMBER_OF_BLOCKS_TO_REQUEST blocks after (including) the appliaction.playClock.gettime
		if (segment == null) {
			logger.warn(" segment (" + this.segment.getSegmentIdentifier().toString() + ") not found here... something wrong!? (1)");
			return null;
		}
		
		
		//checks if the playtime resides in this segment, 
		long playTimeMS = this.liveShiftApplication.getPlayTimeMillis();
		if (playTimeMS==-1) {
			if (logger.isDebugEnabled()) logger.debug("application.getPlayClock() is still -1, maybe this is just too early");
			return null;
		}
		long playSegmentNumber = SegmentIdentifier.getSegmentNumber(playTimeMS);
		long thisSegmentNumber = this.getSegmentIdentifier().getSegmentNumber();
		
		if (playSegmentNumber+1 == thisSegmentNumber) {
			//this segment is one ahead of the playing time, which is OK
			
			nextBlocks = segment.getSegmentBlockMap().getNextClearBits(0, NUMBER_OF_BLOCKS_TO_ENQUEUE, MAX_BLOCKS_AHEAD_OF_PLAY_TIME);

			if (logger.isDebugEnabled()) logger.debug(" (+1) next blocks for segment (" + segment.toString() + ") are " + nextBlocks.toString());

		}
		else if (playSegmentNumber == thisSegmentNumber) {
			//the normal case, we are playing and scheduling the same segment
			
			int blockPlayed = SegmentBlock.getBlockNumber(playTimeMS);
			nextBlocks = segment.getSegmentBlockMap().getNextClearBits(blockPlayed, NUMBER_OF_BLOCKS_TO_ENQUEUE, blockPlayed+MAX_BLOCKS_AHEAD_OF_PLAY_TIME);
			
			if (logger.isDebugEnabled()) logger.debug(" (N) next blocks for segment (" + segment.toString() + ") are " + nextBlocks.toString());
		}
		else if (this.nextSegmentRequested.get()) {
			if (logger.isDebugEnabled()) logger.debug("mission accomplished, shutting down BlockScheduler "+this.getSegmentIdentifier().toString());
			
			// mission accomplished
			// this object is useless now
			this.shutdown();
		}
		else {
			if (logger.isDebugEnabled()) logger.debug("keeping this alive just to schedule the next one when play position gets closer");
			//otherwise, it should wait alive until the play position catches up
		}
		
		this.checkIfScheduleNextSegment(this.liveShiftApplication.getPlayTimeMillis());
		
		return nextBlocks;
	}

	/**
	 * checks if it's time to schedule the next one
	 * based on playing time and last scheduled block
	 * 
	 * if it is time, this method will schedule the next one only once
	 * 
	 * @param playTimeMs
	 */
	private void checkIfScheduleNextSegment(long playTimeMs) {
		if (logger.isDebugEnabled()) logger.debug("checkIfScheduleNextSegment("+playTimeMs+") :: "+this.segment);

		//if not playing this segment, do not schedule
		if (this.segment.getSegmentIdentifier().getSegmentNumber() > SegmentIdentifier.getSegmentNumber(playTimeMs)) {
			if (logger.isDebugEnabled()) logger.debug("play time is not there yet, not scheduling next segment yet");
			return;
		}
		else if (this.segment.getSegmentIdentifier().getSegmentNumber() < SegmentIdentifier.getSegmentNumber(playTimeMs)) {
			//if already playing the next one -- may happen in rare occasions when the VideoPlayer's play scheduled task takes too long to play by the end of the segment
			if (logger.isDebugEnabled()) logger.debug("it's time to schedule the next segment! (based on play time)");

			this.scheduleNextSegment();
		}
			
		//if it could cause a timeout (due to blocks not being requested because they belong in the future, thus currentTime in there) on the other side, no way!
		long currentTimeMillis = Clock.getMainClock().getTimeInMillis(false);
		if (this.segment.getSegmentIdentifier().getEndTimeMS() - currentTimeMillis > UploadSlot.INACTIVITY_TIMEOUT_S*1000) {
			if (logger.isDebugEnabled()) logger.debug("it could cause a timeout, not scheduling next one yet");
			return;
		}

		//if downloading (and playing) near the end, requests next
		int triggerBlock = (int)(SCHEDULE_NEXT_SEGMENT_WHEN_REQUESTED_SHARE*Configuration.SEGMENT_SIZE_MS/Configuration.SEGMENTBLOCK_SIZE_MS);
		if (this.segment.getSegmentBlockMap().getLastSetBit(SegmentBlock.getBlockNumber(playTimeMs)) >= triggerBlock) {
			if (logger.isDebugEnabled()) logger.debug("it's time to schedule the next segment! (based on newest block)");
			this.scheduleNextSegment();
		}
		else
			if (logger.isDebugEnabled()) logger.debug("lastbitset " +this.segment.getSegmentBlockMap().getLastSetBit()+ " < "+ triggerBlock+ " triggerBlock");
		
	}
	
	private void scheduleNextSegment() {
		if (this.nextSegmentRequested.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) logger.debug("scheduling the next segment!");

			this.liveShiftApplication.getTuner().scheduleNextSegment(this.segment.getSegmentIdentifier());
		}
		else
			if (logger.isDebugEnabled()) logger.debug("it was already scheduled");
	}

	private Boolean addBlocksToQueue(BitSet blockSet) {

		if (blockSet==null) {
			logger.warn("null blockset received as parameter -- not tuned yet?");
			return false;
		}
	
		//requests blocks that don't reside in the future, haven't been queued, and haven't arrived yet
		long curTimeMs = Clock.getMainClock().getTimeInMillis(false);
		
		for (int block = blockSet.nextSetBit(0); block >= 0 && this.segment.getSegmentIdentifier().getBlockStartTimeMs(block) < curTimeMs-1; block = blockSet.nextSetBit(block+1))
			if (!this.segment.getSegmentBlockMap().get(block)) {
				BlockRequest blockRequest = new BlockRequest(this.segment.getSegmentIdentifier(), block);
				if (!this.liveShiftApplication.getTuner().getBlockRequestQueue().isScheduled(blockRequest)) {
					if (logger.isDebugEnabled()) logger.debug("scheduling b#"+block+" from "+this.toString());
					
					//puts in the priority queue
					this.highestScheduledBlockTime = Math.max(this.highestScheduledBlockTime, this.segment.getSegmentIdentifier().getBlockStartTimeMs(block));
					this.liveShiftApplication.getTuner().getBlockRequestQueue().offer(blockRequest);
					
					//just for synchronization
					this.firstBlockScheduled = true;
				}
			}
		return true;
	}

	@Override
	public String toString() {
		return "BS si:(" + this.segment.getSegmentIdentifier().toString() + ")";
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return this.segment.getSegmentIdentifier();
	}

	public void scheduleNow() {
		this.scheduledExecutorService.scheduleNow();
	}

	public boolean isRunning() {
		return this.running;
	}

	public long getHighestScheduledBlockTimeMs() {
		return this.highestScheduledBlockTime;
	}

}
