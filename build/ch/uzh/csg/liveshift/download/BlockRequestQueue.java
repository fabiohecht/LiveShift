package net.liveshift.download;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.util.ExecutorPool;


public class BlockRequestQueue {

	final private static Logger logger = LoggerFactory.getLogger(BlockRequestQueue.class);

	private static final int RESCHEDULE_DELAY_TIMES_NEGHBOR_AVERAGE_BLOCK_REPLY_TIME = 2;
	
	private BlockPriorityBlockingQueue<BlockRequest> priorityQueue = new BlockPriorityBlockingQueue<BlockRequest>();
	private Map<BlockRequest, ScheduledFuture<?>> takenBlocks = new HashMap<BlockRequest, ScheduledFuture<?>>();
	
	public void initialize() {
		this.priorityQueue = new BlockPriorityBlockingQueue<BlockRequest>();
		this.takenBlocks = new HashMap<BlockRequest, ScheduledFuture<?>>();
	}

	public void offer(final BlockRequest blockRequest) {
		if (this.priorityQueue==null) {
			logger.error("call initialize() before offer()");
			LiveShiftApplication.quit("call initialize() before offer()");
		}
		
		this.priorityQueue.offer(blockRequest);
	}
	
	/**
	 * Gets from the queue a block request that matches the blockMap provided
	 * 
	 * @param neighbor
	 * @param averageNeighborResponseTimeMillis 
	 * @return
	 * @throws InterruptedException
	 */
	public BlockRequest take(final Neighbor neighbor, final long averageNeighborResponseTimeMillis) throws InterruptedException {
		if (this.priorityQueue==null) {
			logger.error("call initialize() before take()");
			LiveShiftApplication.quit("call initialize() before take()");
		}
		
		final BlockRequest blockRequest = this.priorityQueue.take(neighbor);
		
		// schedules its rescheduling
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				try {
					if (logger.isDebugEnabled()) logger.debug("rescheduling " + blockRequest);
					
					offer(blockRequest);
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					e.printStackTrace();
				}
			}
		};
		
		long rescheduleTimeMillis = averageNeighborResponseTimeMillis * RESCHEDULE_DELAY_TIMES_NEGHBOR_AVERAGE_BLOCK_REPLY_TIME;
		if (averageNeighborResponseTimeMillis==0)
			rescheduleTimeMillis = Configuration.SEGMENTBLOCK_SIZE_MS * RESCHEDULE_DELAY_TIMES_NEGHBOR_AVERAGE_BLOCK_REPLY_TIME * 2;
		
		rescheduleTimeMillis = Math.max(rescheduleTimeMillis, 2 * Configuration.SEGMENTBLOCK_SIZE_MS);

		synchronized (this.takenBlocks) {
			ScheduledFuture<?> scheduledFuture = ExecutorPool.getScheduledExecutorService().schedule(runner, rescheduleTimeMillis, TimeUnit.MILLISECONDS);
		
			this.takenBlocks.put(blockRequest, scheduledFuture);
		}
		return blockRequest;
	}
	
	//called if the piece has arrived or interest changed
	public void remove(final BlockRequest blockRequest) {
		
		if (logger.isDebugEnabled()) logger.debug("removing("+blockRequest+") -- piece has arrived! (or we are cleaning up)");
		
		synchronized (this.takenBlocks) {	
			this.priorityQueue.remove(blockRequest);
			ScheduledFuture<?> scheduledFuture = this.takenBlocks.get(blockRequest);
			if (scheduledFuture != null)
				scheduledFuture.cancel(false);
			this.takenBlocks.remove(blockRequest);
		}
	}
	
	public boolean hasInterestingBlocks(Neighbor neighbor) {
		return this.priorityQueue.hasInterestingBlocks(neighbor);
	}
	
	@Override
	public String toString() {
		return this.priorityQueue.toString();
	}

	public void notifyLock() {
		this.priorityQueue.notifyLock();
	}

	public void reset() {
		synchronized (this.takenBlocks) {	
			Collection<BlockRequest> collection = new HashSet<BlockRequest>();
			if (this.priorityQueue != null)
				this.priorityQueue.drainTo(collection);
			for (BlockRequest blockRequest : collection)
				this.remove(blockRequest);
		}
	}
	
	public boolean isScheduled(final BlockRequest blockRequest) {
		synchronized (this.takenBlocks) {	
			return this.priorityQueue.contains(blockRequest) || this.takenBlocks.containsKey(blockRequest);
		}
	}
	
	/**
	 * gets rid of blocks requests behind play time
	 */
	public void cleanup(final long playTimeMillis) {
		if (logger.isDebugEnabled()) logger.debug("cleaning up ("+playTimeMillis+")");
		
		synchronized (this.takenBlocks) {
			List<BlockRequest> queueElements = this.priorityQueue.snapshot();
			Set<BlockRequest> elementsToRemove = new HashSet<BlockRequest>();
			
			for (BlockRequest blockRequest : queueElements)
				if (SegmentBlock.getStartTimeMillis(playTimeMillis) > SegmentBlock.getStartTimeMillis(blockRequest.getSegmentIdentifier(), blockRequest.getBlockNumber()))
					elementsToRemove.add(blockRequest);
			
			for (BlockRequest blockRequest : elementsToRemove)
				this.remove(blockRequest);
			
		}
	}
	
	public List<BlockRequest> snapshot() {
		return this.priorityQueue.snapshot();
	}
	
	public boolean isEmpty() {
		return this.priorityQueue.isEmpty();
	}
}
