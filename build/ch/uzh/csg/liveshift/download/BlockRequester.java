package net.liveshift.download;

import java.nio.channels.ClosedByInterruptException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.Stats;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;

/**
 * There is one BlockRequester per peer granting an upload slot (=one segment).
 * 
 * BlockRequesters take a block from the priority queue, and send the request
 * to the peer.
 * 
 * @author fabio
 *
 */
public class BlockRequester {

	final private static Logger logger = LoggerFactory.getLogger(BlockRequester.class);

	private static final int REPLY_TIME_WINDOW_SIZE	= 5;
	private static final int NUMBER_SIMULTANEOUS_REQUESTS = 2;
	
	final private Neighbor neighbor;
	final private Segment segment;
	final private Tuner tuner;

	final private Map<Integer, Long> blockTimes = new ConcurrentHashMap<Integer, Long>();
	private boolean	running;
	private long[] replyTimeWindow;
	private int replyTimeWindowPosition;
	
	final private BlockRequesterWorker[] workers;
	
	private Stats stats;

	
	private class BlockRequesterWorker {
		
		ReentrantLock dontInterruptMe = new ReentrantLock();
		final private Thread thread;
		final private int number;

		Runnable runner = new Runnable() {
			@Override
			public void run() {
				// gets request from queue, chooses peer, sends request, and starts over
				while (running) {
					try {
						processBlockRequest();
					} catch (InterruptedException e) {
						if (logger.isDebugEnabled()) logger.debug("interrupted (InterruptedException)");
						continue;
					} catch (ClosedByInterruptException e) {
						if (logger.isDebugEnabled()) logger.debug("interrupted (ClosedByInterruptException)");
						continue;
					} catch (Exception e) {
						// just so it doesn't die silently if an unhandled exception happened
						logger.error("error in BlockRequester: "+e.getMessage());
						e.printStackTrace();
					}

			    }
			}
		};
		
		public BlockRequesterWorker(int number) {
			this.number = number;
			this.thread = new Thread(this.runner);
			this.thread.setName("BlockRequester ["+neighbor.getPeerId().getName()+","+segment.getSegmentIdentifier().getSegmentNumber()+":"+segment.getSegmentIdentifier().getSubstream()+"#"+(number)+"]");
			this.thread.start();
		}

		private void processBlockRequest() throws InterruptedException, ClosedByInterruptException {
			if (logger.isDebugEnabled()) logger.debug("Request Q snapshot: "+tuner.getBlockRequestQueue().toString()+" neighbor bm:"+neighbor.getBlockMap().toString());

			final BlockRequest blockRequest;
		
			if (!running) return;
			blockRequest = tuner.getBlockRequestQueue().take(neighbor, Math.round(getReplyTimeWindowAverage()));  //this blocks until there is an incoming request and Have message

			this.dontInterruptMe.lock();
			try {
			
				if (!running) return;
				
				if ( blockRequest == null ) {
					logger.warn("got a null BlockRequest from Q to download slot for peer "+neighbor.getPeerId().getName());
					return;
				}
	
				if (logger.isDebugEnabled()) logger.debug("Got [" + blockRequest + "] from queue to download slot for peer "+neighbor.getPeerId().getName());
	
				if (!checkBlock(blockRequest)) {
					if (logger.isDebugEnabled()) logger.debug("skipping block "+blockRequest);
					tuner.getBlockRequestQueue().remove(blockRequest);  //to unschedule rescheduling
				}
				else {
					if (logger.isDebugEnabled()) logger.debug(" sending request for " + blockRequest);
					sendRequest(blockRequest);
				}
				/* probably not necessary anymore, since we check it every 1s
				 * 
				ExecutorPool.getGeneralExecutorService().submit(new Runnable() {
					
					@Override
					public void run() {
						try {
							tuner.checkAndRemoveOldTunedSegment(blockRequest.getSegmentIdentifier());
						}
						catch (Exception e) {
							logger.error("error in checkAndRemoveOldTunedSegment("+blockRequest.getSegmentIdentifier()+"): "+e.getMessage());
							e.printStackTrace();
						}
					}
				});
				*/
			}
			finally {
				this.dontInterruptMe.unlock();
			}
			
			if (logger.isDebugEnabled()) logger.debug("Finished with [" + blockRequest + "] at download slot for peer "+neighbor.getPeerId().getName() +".");

		}

		public void interruptIfNecessary() {
			if (this.dontInterruptMe.tryLock()) {
				if (logger.isDebugEnabled()) logger.debug("will interrupt, so it doesn't stay locked in queue");
				this.thread.interrupt();
				
				this.dontInterruptMe.unlock();
			}
		}
	}

	public BlockRequester(final Segment segment, final Neighbor neighbor, final Tuner tuner) {
		
		if (logger.isDebugEnabled()) logger.debug("new BlockRequester("+segment+","+neighbor+")");
		this.neighbor = neighbor;
		this.tuner = tuner;
		this.segment = segment;

		this.replyTimeWindow = new long[REPLY_TIME_WINDOW_SIZE];
		this.workers = new BlockRequesterWorker[NUMBER_SIMULTANEOUS_REQUESTS];
		
		this.running = true;
		
		ExecutorPool.getGeneralExecutorService().submit(new Runnable() {
			@Override
			public void run() {
				
				try {
					synchronized (workers) {
						for (int i=0; i< NUMBER_SIMULTANEOUS_REQUESTS; i++) {
							
							workers[i] = new BlockRequesterWorker(i);
							
							if (i < NUMBER_SIMULTANEOUS_REQUESTS-1)
								try {
									Thread.sleep(Configuration.SEGMENTBLOCK_SIZE_MS/2);
								} catch (InterruptedException e) {
									logger.warn("interrupted");
								}
						}
					}
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					logger.error("error creating workers:"+e.getMessage());
					e.printStackTrace();
				}
				
			}
		});
	}

	private void sendRequest(BlockRequest blockRequest) throws InterruptedException, ClosedByInterruptException  {
		
		logger.info("sending block request: "+blockRequest);
		this.blockTimes.put(new Integer(blockRequest.getBlockNumber()), Clock.getMainClock().getTimeInMillis(false));

		this.tuner.getVideoSignaling().sendBlockRequest(blockRequest, this.neighbor.getPeerId());
	}
	
	public void blockDownloaded(final int blockNumber) {
		try {
			Long t0 = this.blockTimes.get(blockNumber);
			long tdiff = -1;
			if (t0==null) {
				logger.warn("strange, block request time not found for b#"+blockNumber);
			}
			else {
				tdiff = Clock.getMainClock().getTimeInMillis(false) - t0;
			}			
			
			if (tdiff>-1) {
				this.replyTimeWindow[replyTimeWindowPosition++ % REPLY_TIME_WINDOW_SIZE] = tdiff;
				
				logger.info("getting b#"+blockNumber+" from "+this.neighbor+" took "+tdiff+" ms, avg for this peer is "+this.getReplyTimeWindowAverage());
			}
		}
		catch (NullPointerException npe) {
			//for some reason it was happening
			logger.warn("NullPointerException at blockDownloaded("+blockNumber+") -- strange but seldom happened. blockTimes is null? "+(this.blockTimes==null) + "blocktimes.get("+blockNumber+") is null?"+(this.blockTimes.get(blockNumber)==null));
			npe.printStackTrace();
		}
	}
	
	private boolean checkBlock(BlockRequest blockRequest) {
		SegmentIdentifier segmentIdentifier = blockRequest.getSegmentIdentifier();
		int blockNumber = blockRequest.getBlockNumber();
		
		//checks if the segmentIdentifier is correct
		if (!this.segment.getSegmentIdentifier().equals(blockRequest.getSegmentIdentifier())) {
			logger.warn("block has the wrong segment identifier.");
			//TODO punish peer for breaking protocol
			
			//stats
			this.getStats().blockStatChange(blockRequest.getSegmentIdentifier(), blockRequest.getBlockNumber(), 3);
			
			return false;
		}
		
		//checks if we already got the block
		if (this.segment.getSegmentBlockMap().get(blockNumber)) {
			if (logger.isDebugEnabled()) logger.debug("block seems to have already gotten here somehow, not requesting.");
			return false;
		}
		
		long playTimeMs = this.tuner.getApplication().getPlayTimeMillis();
		
		//checks whether play position already passed this block
		if (segmentIdentifier.getBlockStartTimeMs(blockNumber) < SegmentBlock.getStartTimeMillis(playTimeMs)) {  //the apparent double conversion is to round correctly the block number 
			if (logger.isDebugEnabled()) logger.debug("block to be requested ["+blockRequest.toString()+"] was already skipped by playback, not requesting ("+segmentIdentifier.getBlockStartTimeMs(blockNumber) +"<"+SegmentBlock.getStartTimeMillis(playTimeMs));

			//stats
			this.getStats().blockStatChange(blockRequest.getSegmentIdentifier(), blockRequest.getBlockNumber(), 3);
			
			return false;
		}
		else {
			if (logger.isDebugEnabled()) logger.debug("block to be requested ["+blockRequest.toString()+"] was NOT already skipped by playback, requesting ("+segmentIdentifier.getBlockStartTimeMs(blockNumber) +">="+SegmentBlock.getStartTimeMillis(playTimeMs));
			
			//stats
			this.getStats().blockStatChange(blockRequest.getSegmentIdentifier(), blockRequest.getBlockNumber(), 2);
			
			return true;
		}
	}
	
	public void shutdown() {
		if (logger.isDebugEnabled()) logger.debug("shutting down BR ["+this.toString()+"]");
		this.running = false;

		/*
		 * interrupts block requesters
		 * but should leave them running if they are waiting for the reply (REJECTED) so the flow control gets the message with the number
		 */ 
		 
		synchronized (this.workers) {
			for (int i=0; i < this.workers.length; i++)
				if (this.workers[i]!=null)
					this.workers[i].interruptIfNecessary();
		}
		
	}
	
	private double getReplyTimeWindowAverage() {
		double avg = 0F;
		for (int i = 0; i < Math.min(this.replyTimeWindowPosition, REPLY_TIME_WINDOW_SIZE); i++)
			avg += this.replyTimeWindow[i];
		avg /= Math.min(this.replyTimeWindowPosition, REPLY_TIME_WINDOW_SIZE);
		return avg;
	}

	private Stats getStats() {
		if (this.stats==null)
			this.stats = this.tuner.getApplication().getStats();
		return this.stats;
	}
	
	@Override
	public String toString() {
		return neighbor.getPeerId().getName()+","+segment.getSegmentIdentifier().getSegmentNumber()+":"+segment.getSegmentIdentifier().getSubstream();
	}

	public Neighbor getNeighbor() {
		return this.neighbor;
	}
	
	public boolean hasBlock(int blockNumber) {
		return this.neighbor.hasBlock(blockNumber);
	}
}

