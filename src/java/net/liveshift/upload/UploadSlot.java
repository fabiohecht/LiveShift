package net.liveshift.upload;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.PeerId;
import net.liveshift.signaling.messaging.BlockReplyMessage;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.signaling.messaging.BlockReplyMessage.BlockReplyCode;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Represents an upload slot
 * 
 * @author fabio
 * 
 */

public class UploadSlot extends Thread implements Comparable<UploadSlot> {
	
	final private static Logger logger = LoggerFactory.getLogger(UploadSlot.class);

	static final int GRANTED_TIMEOUT_MILLIS = (int) (1.20 * Configuration.SEGMENT_SIZE_MS);
	static final long UNGRANTED_INTERESTED_TIMEOUT_MILLIS = 10000L;
	static final long UNGRANTED_NOT_INTERESTED_TIMEOUT_MILLIS = 5000L;
	public static final int	 INACTIVITY_TIMEOUT_S = 4;
	private static final float INACTIVE_PEER_REPUTATION_MULTIPLIER	= .9F;
	
	private final int slotNumber;
	private final UploadSlotManager uploadSlotManager;
	private final MovingAverage failureCounter;
	final private AtomicBoolean lock = new AtomicBoolean(false);
	
	private boolean running;
	private boolean granted = true;
	
	private Subscriber subscriber;
	private long lastActivityMillis = 0L;
	private final MovingAverage activityAverage;
	private final MovingAverage bandwidthAverage;
	private ScheduledFuture<?> scheduledInactivityTask;
	private long grantedTimeMillis;
	
	public UploadSlot(int slotNumber, UploadSlotManager uploadSlotManager) {
		this.slotNumber = slotNumber;
		this.uploadSlotManager = uploadSlotManager;
		this.failureCounter = new MovingAverage(Clock.getMainClock(),10, 1000);
		this.activityAverage = new MovingAverage(Clock.getMainClock(),3, 1000);
		this.bandwidthAverage = new MovingAverage(Clock.getMainClock(),5, 1000);

		this.setName("UploadSlot#" + slotNumber);

		this.running = true;
	}

	@Override
	public void run() {

		// gets peer from queue, sends request, and starts over

		while (running) {
			// Stats
			this.uploadSlotManager.updateStats();

			try {

				if (logger.isDebugEnabled()) logger.debug("USR Q snapshot: " + this.uploadSlotManager.getQueueSnapshotString());

				//this blocks until there is an incoming request
				Subscriber subscriber = null;
				try {
					this.subscriber = null;
					
					subscriber = this.uploadSlotManager.getNextUploadSlotRequest();
					
					synchronized (this.lock) {
						if (logger.isDebugEnabled()) logger.debug("Got [" + subscriber + "] from USR queue to US#" + this.slotNumber);
		
						this.subscriber = subscriber;
	
						if (subscriber == null) {
							logger.warn("got a null UploadSlotRequest from Q to US#" + this.slotNumber);
							continue;
						}
						
						// TODO this is not going to happen anymore, since the queue was modified not to give away the slot if we don't have any blocks in there
						if (!subscriber.canSetSlotGranted()) {
							logger.warn(this.uploadSlotManager.peerName + " was about to grant an UploadSlotRequest [" + subscriber + "] for a segment we don't have -- slot not granted");
							this.uploadSlotManager.getVideoSignaling().sendSubscribed(subscriber.getSegmentIdentifier(), null, 0, subscriber.getRequesterPeerId(), false);
		
							this.uploadSlotManager.setNotGranted(subscriber);
		
							continue;
						}
		
						subscriber.setGranted(true);
						this.uploadSlotManager.setGranted(subscriber, this);

						// sends message to peer informing that it was awarded a slot
						this.uploadSlotManager.getVideoSignaling().sendGranted(subscriber.getSegmentIdentifier(), GRANTED_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_S*1000, subscriber.getRequesterPeerId(), false);
						
						// schedules task that checks for inactivity
						this.lastActivityMillis = Clock.getMainClock().getTimeInMillis(false);
						
						final Subscriber uploadSlotRequestFinal = subscriber;
						Runnable inactivityRunner = new Runnable() {
							@Override
							public void run() {
								try {
									long currentTime = Clock.getMainClock().getTimeInMillis(false);
		
									if (uploadSlotRequestFinal!=null && lastActivityMillis <= currentTime - INACTIVITY_TIMEOUT_S * 1000 /*&& uploadSlotManager.hasGrantableUploadSlotRequestsInQueue()*/) {
										if (logger.isDebugEnabled()) logger.debug("inactivity detected in [" + uploadSlotRequestFinal + "], US#" + slotNumber + " will be freed.");
										ungrantSlot(true, true, false, false, uploadSlotRequestFinal);
									}
								} catch (Exception e) {
									// just so it doesn't die silently if an unhandled
									// exception happened
									if (logger.isDebugEnabled()) logger.debug("caught exception: "+e.getMessage());
		
									e.printStackTrace();
								}
		
							}
						};
						this.scheduledInactivityTask = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(inactivityRunner, INACTIVITY_TIMEOUT_S, 1, TimeUnit.SECONDS);
		
						// stats
						this.uploadSlotManager.updateStats();
	
						this.grantedTimeMillis = Clock.getMainClock().getTimeInMillis(false);
						this.granted = true;
					}
				}
				catch (InterruptedException e) {
					if (logger.isDebugEnabled()) logger.debug("interrupted while taking from USReq Q or granting");
				}
				
				// processes queue
				while (this.granted) {
				
					// answers a request
					if (logger.isDebugEnabled()) logger.debug("will take from IBR queue");
					
					BlockRequestMessage incomingBlockRequest = null;
					long timeGot;
					try {
	
						if (this.subscriber==null)
							break;
						
						incomingBlockRequest = this.uploadSlotManager.getBlockRequest(subscriber);
	
						timeGot=Clock.getMainClock().getTimeInMillis(false);
						
						if (incomingBlockRequest == null) {
							logger.warn("got a null incomingBlockRequest");
							continue;
						}
						else if (incomingBlockRequest.isProcessed()) {
							//rejected by flow control
							this.handleBlockRequest(incomingBlockRequest, true);
						}
						else {
							if (logger.isDebugEnabled()) logger.debug("got " + incomingBlockRequest);
							
							this.handleBlockRequest(incomingBlockRequest, false);
						}
					}
					finally {
						// notifies
						synchronized (this.lock) {
							if (incomingBlockRequest!=null)
								synchronized (incomingBlockRequest) {
									if (null==incomingBlockRequest.getReplyMessage()) {
										if (logger.isDebugEnabled()) logger.debug("Block replying (REJECTED) to "+incomingBlockRequest.getSender());
		
										incomingBlockRequest.setReplyMessage(this.uploadSlotManager.getVideoSignaling().getBlockReply(BlockReplyCode.REJECTED, new SegmentBlock(incomingBlockRequest.getSegmentIdentifier(), incomingBlockRequest.getBlockNumber()), incomingBlockRequest.getSender()), incomingBlockRequest.getSender());
									}
									incomingBlockRequest.setProcessed();
									incomingBlockRequest.notify();
								}
						}
					}

					if (logger.isDebugEnabled()) logger.debug("done with " + incomingBlockRequest+", took="+(Clock.getMainClock().getTimeInMillis(false)-timeGot));

					// Stats
					this.uploadSlotManager.getVideoSignaling().getStats().updateIncomingRequest((BlockReplyMessage) incomingBlockRequest.getReplyMessage(), incomingBlockRequest.getSender());			
					this.uploadSlotManager.updateStats();

					// checks timeout
					if (!this.isInterrupted() && Clock.getMainClock().getTimeInMillis(false) >= this.grantedTimeMillis + GRANTED_TIMEOUT_MILLIS) {
						if (logger.isDebugEnabled()) logger.debug("finished with [" + subscriber + "],  US#" + this.slotNumber + " will be replaced.");
						ungrantSlot(true, false, false, null, subscriber);
						granted = false;
					}
					
				}
			} catch (InterruptedException e) {
				if (logger.isDebugEnabled()) logger.debug("interrupted");
				
				synchronized (this.lock) {
					if (logger.isDebugEnabled()) logger.debug("about to continue");
					continue;
				}
			} catch (Exception e) {
				logger.error("caught exception: "+e.getMessage());

				e.printStackTrace();
			}
		}
	}
	
/**
 * kicks out the current UploadSlotRequest from this slot, taking care of all details
 * 
 * @param penalize whether to penalize the peer with lower reputation (normally used in case of timeout)
 * @param backInQueueOrRejected if it should send BACK_IN_QUEUE or REJECTED messages to the peer losing its slot (used when the decision of kicking out came from this peer)
 * @param interrupt whether to interrupt the UploadSlot thread, immediately kicking out the peer
 * @param sendDisconnect whether to send Disconnect to the peer (normally if we are shutting down the application)
 * @param setInterested whether to also setInterested on the request (null=don't set, true/false = set)
 * @param uploadSlotRequestToUngrant to avoid concurrency problems when the slot was just ungrant, you can specify which request is supposed to be kicked out (or null if it doesn't matter)
 * @param finalNewSubscriber 
 */
	protected void ungrantSlot(final boolean backInQueueOrRejected, final boolean interrupt, final boolean sendDisconnect, final Boolean setInterested, final Subscriber uploadSlotRequestToUngrant) {
		this.ungrantSlot(backInQueueOrRejected, interrupt, sendDisconnect, setInterested, uploadSlotRequestToUngrant, null);
	}
	
	protected void ungrantSlot(final boolean backInQueueOrRejected, final boolean interrupt, final boolean sendDisconnect, final Boolean setInterested, final Subscriber uploadSlotRequestToUngrant, final Subscriber finalNewSubscriber) {

		if (logger.isDebugEnabled()) logger.debug("in ungrantSlot(" + backInQueueOrRejected + "," + interrupt + "," + sendDisconnect + "," + setInterested + ")");
		
		if (!this.lock.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) logger.debug("is currently granting or ungranting the slot, not ungranting it");
			return;
		}
		else
			try {
				synchronized (this.lock) {

					if (logger.isDebugEnabled()) logger.debug("got lock");
	
					Subscriber subscriber = this.subscriber;
					
					if (uploadSlotRequestToUngrant!=null && !uploadSlotRequestToUngrant.equals(subscriber)) {
						if (logger.isDebugEnabled()) logger.debug("slot is not given to ("+uploadSlotRequestToUngrant+") anymore. not ungranting.");
						
						return;
					}
					if (finalNewSubscriber!=null && !finalNewSubscriber.mayBeGranted() && !finalNewSubscriber.isGranted()) {
						if (logger.isDebugEnabled()) logger.debug("slot may not be granted to new subscriber ("+finalNewSubscriber.mayBeGranted()+") or subscriber already has a slot ("+finalNewSubscriber.isGranted()+").");
						
						return;
					}
					
					if (this.scheduledInactivityTask != null)
						this.scheduledInactivityTask.cancel(false);
					
					if (interrupt) {
						this.granted = false;
						this.interrupt();
					}
					
					if (subscriber != null) {
						
						// if the request is not the same that was here before,
						// sends message to peer informing that it lost the slot
						
						/*
						if (penalize) {
							// if preempted due to inactivity,
							// penalizes peer by reducing the chance of it getting an upload slot here in the future
							this.uploadSlotManager.getVideoSignaling().getIncentiveMechanism().penalizePeer(subscriber.getRequesterPeerId(), INACTIVE_PEER_REPUTATION_MULTIPLIER);
						}*/
						
						if (backInQueueOrRejected) {
							if (setInterested!=null)
								subscriber.setInterested(setInterested);
							
							//adds back to queue
							//(blocks to avoid having BIQ racing condition with GRANTED if another slot gets the request immediately)
							subscriber.setBlocked(true);  //another slot may not get this specific request yet (avoids race condition)

							subscriber.setGranted(false);

							int timeoutMillis = this.uploadSlotManager.addSubscriberGetTimeoutMillis(subscriber);
							if (timeoutMillis > 2000) {  //considers peer NOT INTERESTED, otherwise it would end up getting a slot again and possible timing out
								this.uploadSlotManager.getVideoSignaling().sendQueued(subscriber.getSegmentIdentifier(), timeoutMillis, subscriber.getRequesterPeerId(), true);
							}
							else //if there would be no time left anyway, sends NOT SUBSCRIBED
								this.uploadSlotManager.getVideoSignaling().sendSubscribed(subscriber.getSegmentIdentifier(), null, 0, subscriber.getRequesterPeerId(), true);
						}
						else {
							if (sendDisconnect)
								this.uploadSlotManager.getVideoSignaling().sendDisconnect(subscriber.getSegmentIdentifier(), subscriber.getRequesterPeerId(), false, true);
								
							subscriber.setGranted(false);
						}
						
						this.uploadSlotManager.setNotGranted(subscriber);
						
						this.subscriber = null;
					}
					this.resetBlockRequestQueue();
		
					//frees it so a slot may take it (but only in a few seconds, to give a chance to lower-rated peers that may use the slot better)
					if (subscriber!=null) {
						if (logger.isDebugEnabled()) logger.debug("blocking request for 2s");

						subscriber.setBlocked(2000);
					}
					
					if (logger.isDebugEnabled()) logger.debug("done with ungrantSlot(" + backInQueueOrRejected + "," + interrupt + "," + sendDisconnect + ")");
				}
			
			}
			finally {
				this.lock.set(false);
			}
	}


	private void handleBlockRequest(final BlockRequestMessage blockRequestMessage, final boolean sendRejected) {

		try {
			this.lastActivityMillis = Clock.getMainClock().getTimeInMillis(false);

			if (sendRejected) {
				blockRequestMessage.setReplyMessage(this.uploadSlotManager.getVideoSignaling().getBlockReply(BlockReplyCode.REJECTED, new SegmentBlock(blockRequestMessage.getSegmentIdentifier(), blockRequestMessage.getBlockNumber()), blockRequestMessage.getSender()), blockRequestMessage.getSender());
			}
			else {
				PeerId peerId = blockRequestMessage.getSender();
				
				// verifies availability of resource
	
				// gets local data which was requested
				SegmentStorage segmentStorage = this.uploadSlotManager.getVideoSignaling().getSegmentStorage();
				SegmentBlock segmentBlock = segmentStorage.getSegmentBlock(blockRequestMessage.getSegmentIdentifier(), blockRequestMessage.getBlockNumber());
				Segment localSegment = segmentStorage.getSegment(blockRequestMessage.getSegmentIdentifier());
	
				// Do we have what was requested?
				boolean availableLocally = (segmentBlock != null && localSegment != null && localSegment.getSegmentBlockMap() != null && !localSegment
						.getSegmentBlockMap().isEmpty());
	
				if (!availableLocally || this.uploadSlotManager.getVideoSignaling().isFreeRider()) {
					//block being requested is not available (yet?)
					//will wait a bit at the queue (BlockRequestPipeline) before ending up here
					
					if (logger.isDebugEnabled()) logger.debug("Block replying (DONT_HAVE) to "+peerId);
					
					// Send not available message
					blockRequestMessage.setReplyMessage(this.uploadSlotManager.getVideoSignaling().getBlockReply(BlockReplyCode.DONT_HAVE, new SegmentBlock(blockRequestMessage.getSegmentIdentifier(), blockRequestMessage.getBlockNumber()), peerId), peerId);
	
				} else {
	
					if (logger.isDebugEnabled()) logger.debug("Block replying successfuly (GRANTED!) to "+peerId);
					
					this.activityAverage.inputValue(1);
					this.bandwidthAverage.inputValue(segmentBlock.getPacketSizeBytes());
					System.out.println("ul#"+this.slotNumber+" activity average: "+this.activityAverage.toString()+" bandwidth: "+this.bandwidthAverage.toString());  //TODO debugging this thing
					
					//decreases reputation of peer
					this.uploadSlotManager.getVideoSignaling().getIncentiveMechanism().decreaseReputation(peerId, 1, SegmentBlock.getStartTimeMillis(blockRequestMessage.getSegmentIdentifier(), blockRequestMessage.getBlockNumber()));
					
					//increases counter of sent blocks
					this.uploadSlotManager.incrementSentBlocks(peerId);
					
					// enforces rate
					this.uploadSlotManager.getVideoSignaling().getUploadSlotManager().enforceRate(blockRequestMessage.getSegmentIdentifier().getChannel(), segmentBlock.getPacketSizeBytes());
					
					// reply
					blockRequestMessage.setReplyMessage(this.uploadSlotManager.getVideoSignaling().getBlockReply(BlockReplyCode.GRANTED, segmentBlock, peerId), peerId);
				}
	
			}
		}
		catch (InterruptedException e) {
			logger.debug("interrupted");
		}
		catch (Exception e) {
			logger.error("exception caught: "+e.getMessage());
		}
		
	}
	
	void shutdown(final boolean backInQueue, final boolean sendDisconnect) {

		this.running = false;

		this.ungrantSlot(backInQueue, true, sendDisconnect, null, null);
	}

	/**
	 * interrupts threads that might be waiting on the locks
	 */
	private void resetBlockRequestQueue() {
		if (logger.isDebugEnabled()) logger.debug("in resetBlockRequestQueue()");
		
		// rejects all pending requests
		Collection<BlockRequestMessage> leftRequests = this.uploadSlotManager.getAllPendingRequests(this.subscriber);
		for (BlockRequestMessage incomingBlockRequest : leftRequests) {
			synchronized (incomingBlockRequest) {
				if (logger.isDebugEnabled()) logger.debug("emptying pipeline, replying REJECTED to "+incomingBlockRequest);
				
				this.handleBlockRequest(incomingBlockRequest, true);
			}
		}
	}
/*
	void addToBlockRequestQueue(final IncomingBlockRequest blockRequest) throws InterruptedException {

		if (!this.incomingBlockRequestQueue.offer(blockRequest)) {
			logger.warn(blockRequest + " NOT offered OK, will reply REJECTED");
			blockRequest.setReply(Reply.REJECTED);
			return;
		}

		if (logger.isDebugEnabled()) logger.debug(blockRequest + " offered OK, will wait for upload slot to proccess it");

		// it's notified after it's processed
		synchronized (blockRequest) {
			if (logger.isDebugEnabled()) logger.debug("in sync2");
			while (true) {
				blockRequest.wait(1000);
				if (blockRequest.isProcessed()) {
					if (logger.isDebugEnabled()) logger.debug("waited OK, will break");
					break;
				}
				if (this.getUploadSlotRequest()==null || 
						!blockRequest.getPeerId().equals(this.getUploadSlotRequest().getRequesterPeerId()) ||
						!blockRequest.getSegmentIdentifier().equals(this.getUploadSlotRequest().getSegmentIdentifier())) {
					if (logger.isDebugEnabled()) logger.debug("lost slot while waiting. will break and reply REJECTED.");
					blockRequest.setReply(Reply.REJECTED);
					break;
				}
				if (logger.isDebugEnabled()) logger.debug("still waiting: ["+blockRequest+"] for US:"+this);
				
			}
		}
	}
*/
	int getSlotNumber() {
		return this.slotNumber;
	}

	int getTimeoutMillis() {
		return (GRANTED_TIMEOUT_MILLIS + (int)(this.grantedTimeMillis - Clock.getMainClock().getTimeInMillis(false)));
	}

	Subscriber getUploadSlotRequest() {
		return this.subscriber;
	}

	@Override
	public String toString() {
		return "US#" + this.slotNumber + " request:" + this.subscriber+" to:"+this.getTimeoutMillis();
	}

	public void signalFailure(final Subscriber subscriber) {
		if (logger.isDebugEnabled()) logger.debug("signaling failure");

		// tries to send ping messages to peer: if fails, gets rid of it
		// (if it fails, sendPing will call this function, watch out for endless
		// loops, potentially with stack overflow)
		this.failureCounter.inputValue(1);

		if (logger.isDebugEnabled()) logger.debug("failure counter " + this.failureCounter.toString());

		if (this.failureCounter.getSum() >= 3) {
			if (logger.isDebugEnabled()) logger.debug("failure threshold passed, kicking out peer " + subscriber);

			this.ungrantSlot(false, true, true, null, subscriber);
		} 
		else if (subscriber != null)
			this.uploadSlotManager.getVideoSignaling().sendPing(subscriber.getRequesterPeerId());

	}

	@Override
	public int hashCode() {
		return this.slotNumber;
	}

	@Override
	public boolean equals(Object uploadSlot0) {

		if (uploadSlot0 instanceof UploadSlot) {
			UploadSlot uploadSlot = (UploadSlot) uploadSlot0;

			return this.slotNumber == uploadSlot.slotNumber;
		} else
			return false;

	}

	/*
	 * could be by incentive mechanism (consider currently granted slot)
	 * but now it tends to maintain the one used most recently
	 * (avoids that the same peer gets it and does not use it, which should be prevented by the penalize parameter to ungrantSlot) 
	 */
	@Override
	public int compareTo(UploadSlot o) {
		if (this.equals(o))
			return 0;
		
		if (this.granted) {
			
			float thisAverage = this.activityAverage.getAverage();
			float osAverage = o.activityAverage.getAverage();
			
			if (thisAverage > osAverage)
				return 1;
			if (thisAverage < osAverage)
				return -1;
			
			if (this.subscriber==null) {
				return -1;
			}			
			return (this.subscriber.compareTo(o.subscriber));

		}
		
		return o.slotNumber-this.slotNumber;
	}

}